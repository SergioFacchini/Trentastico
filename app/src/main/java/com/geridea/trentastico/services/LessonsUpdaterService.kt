package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.geridea.trentastico.Config
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.LessonsChangedActivity
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.DiffLessonsListener
import com.geridea.trentastico.network.request.LessonsDiffResult
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ContextUtils
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import java.util.*

private enum class ScheduleType {
    /**
     * We actually fetched all the lessons and diffed them and all was ok
     */
    CHECK_PERFORMED,

    /**
     * We got a network error while trying to fetch lessons
     */
    NETWORK_ERROR,

    /**
     * The update did not happen because it was called too soon (the waiting time still did not
     * expire)
     */
    CALLED_TOO_EARLY
}

class LessonsUpdaterService : Service() {

    private var numRequestsToSend   = 0
    private var numRequestsFinished = 0

    private var updateAlreadyInProgress = false

    override fun onBind(intent: Intent): IBinder? = null //Do not allow binding

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //Check for lessons updates if
        // 1) More than 4 hours have passed since last check
        // 2) In the last check we had no internet and now we have it

        //Rescheduling of the lessons
        // 1) The check has been successful:
        //    Just reschedule at the normal pace
        // 2) We had no internet:
        //    We reschedule the check and check on the connectivity change broadcast.
        // 3) The check was unsuccessful:
        //    Reschedule at a slower rate.
        val starter = intent.getIntExtra(EXTRA_STARTER, STARTER_UNKNOWN)
        if (updateAlreadyInProgress) {
            //The service is already started and it's doing something
            debug("Update already in progress... ignoring update.")
            return Service.START_NOT_STICKY
        } else if (shouldSearchForLessonServiceStart(starter)) {
            updateAlreadyInProgress = true

            if (didWeJustGainInternet(starter) || isUpdateTimeoutElapsed() || isServiceStartForced(starter)) {
                debug("Updating lessons...")
                diffAndUpdateLessonsIfPossible()
            } else {
                debug("Too early to check for updates.")
                rescheduleAndTerminate(ScheduleType.CALLED_TOO_EARLY)
            }
        } else {
            debug("Searching for lesson updates is disabled!")
        }

        return Service.START_NOT_STICKY
    }

    private fun isServiceStartForced(starter: Int): Boolean = starter == STARTER_DEBUGGER

    private fun shouldSearchForLessonServiceStart(starter: Int) =
            AppPreferences.isSearchForLessonChangesEnabled || starter == STARTER_APP_START //#86


    private fun didWeJustGainInternet(starter: Int): Boolean =
            starter == STARTER_NETWORK_BROADCAST
                    && !AppPreferences.wasLastTimesCheckSuccessful
                    && ContextUtils.weHaveInternet(this)

    private fun isUpdateTimeoutElapsed(): Boolean = AppPreferences.nextLessonsUpdateTime <= System.currentTimeMillis()


    private fun rescheduleAndTerminate(scheduleType: ScheduleType) {
        val calendar = calculateAndSaveNextSchedule(scheduleType)

        AppPreferences.nextLessonsUpdateTime = calendar.timeInMillis

        val intent = createIntent(this, STARTER_ALARM_MANAGER)
        val pendingIntent = PendingIntent.getService(this, 0, intent, FLAG_UPDATE_CURRENT)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, calendar.timeInMillis, pendingIntent)

        debug("Scheduled alarm manager to " + CalendarUtils.formatTimestamp(calendar.timeInMillis))

        stopSelf()
    }

    private fun calculateAndSaveNextSchedule(scheduleType: ScheduleType): Calendar =
        when (scheduleType) {
            ScheduleType.CHECK_PERFORMED -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, Config.LESSONS_REFRESH_WAITING_REGULAR)
                calendar
            }

            ScheduleType.NETWORK_ERROR -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, Config.LESSONS_REFRESH_WAITING_AFTER_ERROR)
                calendar
            }

            ScheduleType.CALLED_TOO_EARLY ->
                CalendarUtils.getCalendarWithMillis(AppPreferences.nextLessonsUpdateTime)
        }

    private fun diffAndUpdateLessonsIfPossible() {
        //No internet: cannot diff
        if(!ContextUtils.weHaveInternet(this)){
            debug("No internet. Cannot check for updates.")

            AppPreferences.wasLastTimesCheckSuccessful = false
            rescheduleAndTerminate(ScheduleType.NETWORK_ERROR)
            return
        } else {
            AppPreferences.wasLastTimesCheckSuccessful = true
        }

        //No study course set: cannot diff
        if (!AppPreferences.isStudyCourseSet) {
            //The user has just run the app or reset it's settings. We currently do not have any
            //study course to fetch lessons from, so we just re-plan the check to the next time.
            rescheduleAndTerminate(ScheduleType.CHECK_PERFORMED)
            return
        }

        //Performing the diff
        numRequestsToSend = AppPreferences.extraCourses.size + 1

        //Study course
        val courseName = AppPreferences.studyCourse.courseName
        val lastValidTimestamp = CalendarUtils.debuggableMillis + Config.LESSONS_CHANGED_ANTICIPATION_MS
        Networker.diffStudyCourseLessonsWithCachedOnes(
                lastValidTimestamp, getDiffLessonsListener(courseName)
        )

        //Extra courses
        AppPreferences.extraCourses.forEach {
            Networker.diffExtraCourseLessonsWithCachedOnes(
                    it, lastValidTimestamp, getDiffLessonsListener(it.lessonName)
            )
        }
    }

    private fun getDiffLessonsListener(courseNameToDisplay: String): DiffLessonsListener {
        return object : DiffLessonsListener {

            override fun onBeforeRequestFinished() {
                numRequestsFinished++
            }

            override fun onLessonsDiffed(result: LessonsDiffResult) {
                if (result.hasSomeDifferences && AppPreferences.isNotificationForLessonChangesEnabled) {
                    showLessonsChangedNotification(result, courseNameToDisplay)
                }

                rescheduleAndTerminateIfRequestsFinished(ScheduleType.CHECK_PERFORMED)
            }

            override fun onLessonsLoadingError() {
                AppPreferences.wasLastTimesCheckSuccessful = false
                rescheduleAndTerminateIfRequestsFinished(ScheduleType.NETWORK_ERROR)
            }

            override fun onNoCachedLessons() {
                rescheduleAndTerminateIfRequestsFinished(ScheduleType.CHECK_PERFORMED)
            }
        }
    }

    private fun rescheduleAndTerminateIfRequestsFinished(scheduleType: ScheduleType) {
        if (numRequestsFinished == numRequestsToSend) {
            rescheduleAndTerminate(scheduleType)
        }
    }

    private fun debug(message: String) {
        UIUtils.showToastIfInDebug(this, message)
    }


    private fun showLessonsChangedNotification(diffResult: LessonsDiffResult, courseName: String) {
        val message = when (diffResult.numTotalDifferences) {
            1    -> "È cambiato l'orario di una lezione!"
            else -> "Sono cambiati gli orari di ${diffResult.numTotalDifferences} lezioni!"
        }

        @Suppress("DEPRECATION")
        val notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(message)
                .setContentText(courseName)
                .setColor(resources.getColor(R.color.colorNotification))
                .setAutoCancel(true)


        val intent = Intent(this, LessonsChangedActivity::class.java)
        intent.putExtra(LessonsChangedActivity.EXTRA_DIFF_RESULT, diffResult)

        val resultPendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationBuilder.setContentIntent(resultPendingIntent)

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_LESSONS_CHANGED_ID, notificationBuilder.build())
    }

    companion object {
        val EXTRA_STARTER = "EXTRA_STARTER"

        val STARTER_UNKNOWN = 0
        val STARTER_NETWORK_BROADCAST = 1
        val STARTER_BOOT_BROADCAST = 2
        val STARTER_APP_START = 3
        val STARTER_ALARM_MANAGER = 4
        val STARTER_DEBUGGER = 6

        val NOTIFICATION_LESSONS_CHANGED_ID = 1000

        fun createIntent(context: Context, starter: Int): Intent {
            val intent = Intent(context, LessonsUpdaterService::class.java)
            intent.putExtra(EXTRA_STARTER, starter)
            return intent
        }

    }
}
