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
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.birbit.android.jobqueue.config.Configuration
import com.geridea.trentastico.Config
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.LessonsChangedActivity
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener
import com.geridea.trentastico.network.controllers.listener.WaitForDownloadListenerToSignalAdapter
import com.geridea.trentastico.network.request.LessonsDiffResult
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ContextUtils
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import com.geridea.trentastico.utils.time.WeekInterval
import com.threerings.signals.Signal1
import com.threerings.signals.Signal2
import java.util.*

class LessonsUpdaterService : Service() {

    private var jobManager: JobManager? = null

    private var updateAlreadyInProgress = false

    override fun onCreate() {
        jobManager = JobManager(Configuration.Builder(this).build())

        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? = //Do not allow binding
            null

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
        if (updateAlreadyInProgress) {
            //The service is already started and it's doing something
            UIUtils.showToastIfInDebug(this, "Update already in progress... ignoring update.")
            return Service.START_NOT_STICKY
        } else if (AppPreferences.isSearchForLessonChangesEnabled) {
            updateAlreadyInProgress = true

            val starter = intent.getIntExtra(EXTRA_STARTER, STARTER_UNKNOWN)
            if (shouldUpdateBecauseWeGainedInternet(starter)) {
                UIUtils.showToastIfInDebug(this, "Updating lessons because of internet refresh state...")
                AppPreferences.hadInternetInLastCheck(true)

                diffAndUpdateLessonsIfPossible(object : LessonsDiffAndUpdateListener {
                    override fun onTerminated(successful: Boolean) = scheduleNextStartAndTerminate(if (successful) SCHEDULE_SLOW else SCHEDULE_QUICK)
                })
            } else if (shouldUpdateBecauseOfUpdateTimeout() || startedAppInDebugMode(starter)) {
                UIUtils.showToastIfInDebug(this, "Checking for lessons updates...")
                diffAndUpdateLessonsIfPossible(object : LessonsDiffAndUpdateListener {
                    override fun onTerminated(successful: Boolean) = scheduleNextStartAndTerminate(if (successful) SCHEDULE_SLOW else SCHEDULE_QUICK)
                })
            } else {
                UIUtils.showToastIfInDebug(this, "Too early to check for updates.")
                scheduleNextStartAndTerminate(SCHEDULE_MISSING)
            }
        } else {
            UIUtils.showToastIfInDebug(this, "Searching for lesson updates is disabled!")
        }

        return Service.START_NOT_STICKY
    }

    private fun startedAppInDebugMode(starter: Int): Boolean = Config.DEBUG_MODE && starter == STARTER_APP_START || starter == STARTER_DEBUGGER

    private fun shouldUpdateBecauseWeGainedInternet(starter: Int): Boolean = starter == STARTER_NETWORK_BROADCAST
            && !AppPreferences.hadInternetInLastCheck()
            && ContextUtils.weHaveInternet(this)

    private fun shouldUpdateBecauseOfUpdateTimeout(): Boolean = AppPreferences.nextLessonsUpdateTime <= System.currentTimeMillis()


    private fun scheduleNextStartAndTerminate(scheduleType: Int) {
        val calendar = calculateAndSaveNextSchedule(scheduleType)

        val intent = createIntent(this, STARTER_ALARM_MANAGER)
        val pendingIntent = PendingIntent.getService(this, 0, intent, FLAG_UPDATE_CURRENT)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, calendar.timeInMillis, pendingIntent)

        UIUtils.showToastIfInDebug(this, "Scheduled alarm manager to " + CalendarUtils.formatTimestamp(calendar.timeInMillis))

        stopSelf()
    }

    private fun calculateAndSaveNextSchedule(scheduleType: Int): Calendar {
        val calendar: Calendar
        if (scheduleType == SCHEDULE_MISSING) {
            //Postponing due to alarm manager approximations
            calendar = CalendarUtils.getCalendarWithMillis(AppPreferences.nextLessonsUpdateTime)

            if (Config.DEBUG_MODE) {
                calendar.add(Calendar.SECOND, Config.DEBUG_LESSONS_REFRESH_POSTICIPATION_SECONDS)
            } else {
                calendar.add(Calendar.MINUTE, Config.LESSONS_REFRESH_POSTICIPATION_MINUTES)
            }

        } else {
            calendar = Calendar.getInstance()

            if (Config.DEBUG_MODE && Config.QUICK_LESSON_CHECKS) {
                var timeToAdd = Config.DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS
                if (scheduleType == SCHEDULE_QUICK) timeToAdd /= 2

                calendar.add(Calendar.SECOND, timeToAdd)
            } else {
                var timeToAdd = Config.LESSONS_REFRESH_WAITING_HOURS
                if (scheduleType == SCHEDULE_QUICK) timeToAdd /= 2

                calendar.add(Calendar.HOUR_OF_DAY, timeToAdd)
            }

            AppPreferences.nextLessonsUpdateTime = calendar.timeInMillis
        }
        return calendar
    }

    private fun diffAndUpdateLessonsIfPossible(listener: LessonsDiffAndUpdateListener) {
        if (ContextUtils.weHaveInternet(this)) {
            if (AppPreferences.isStudyCourseSet) {
                //The current and next week
                val intervalToCheck = WeekInterval(0, +1)

                diffLessons(intervalToCheck).connect { diffResult, diffSuccessful ->
                    if (diffResult.isEmpty) {
                        UIUtils.showToastIfInDebug(this@LessonsUpdaterService, "No lesson differences found.")
                    } else {
                        showLessonsChangedNotification(diffResult)
                    }

                    //We've tracked all the updates. Now we have to fetch the eventually missing
                    //schedules so we can be notified if they change in the future.
                    loadMissingLesson(intervalToCheck).connect { updateSuccessful -> listener.onTerminated(diffSuccessful!! && updateSuccessful!!) }
                }
            } else {
                //The user has just run the app or reset it's settings. We currently do not have any
                //study course to fetch lessons from, so we just re-plan the check to the next time.
                listener.onTerminated(false)
            }
        } else {
            UIUtils.showToastIfInDebug(this, "No internet. Cannot check for updates.")
            AppPreferences.hadInternetInLastCheck(false)
            listener.onTerminated(false)
        }
    }

    private fun showLessonsChangedNotification(diffResult: LessonsDiffResult) {
        if (AppPreferences.isNotificationForLessonChangesEnabled) {
            val numDifferences = diffResult.numTotalDifferences

            var message = "È cambiato l'orario di una lezione!"
            if (numDifferences > 1) {
                message = "Sono cambiati gli orari di $numDifferences lezioni!"
            }


            val notificationBuilder = NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(message)
                    .setContentText("Premi qui per i dettagli")
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
    }

    private fun loadMissingLesson(intervalToCheck: WeekInterval): Signal1<Boolean> {
        val job = LoadMissingLessonsJob(intervalToCheck)
        jobManager!!.addJobInBackground(job)
        return job.onCheckTerminated
    }

    private fun diffLessons(intervalToDiff: WeekInterval): Signal2<LessonsDiffResult, Boolean> {
        val job = DiffLessonsJob(intervalToDiff)
        jobManager!!.addJobInBackground(job)
        return job.onCheckTerminated
    }

    private inner class DiffLessonsJob internal constructor(private val intervalToDiff: WeekInterval) : Job(Params(1)), LessonsDifferenceListener {

        internal val onCheckTerminated = Signal2<LessonsDiffResult, Boolean>()
        private val diffAccumulator: LessonsDiffResult

        private var numRequestsSent: Int = 0
        private var numRequestsSucceeded: Int = 0
        private var numRequestsFailed: Int = 0

        init {

            this.numRequestsSent = 0
            this.numRequestsSucceeded = 0
            this.numRequestsFailed = 0

            this.diffAccumulator = LessonsDiffResult()
        }

        @Throws(Throwable::class)
        override fun onRun() = //Loading the current and the next week
                Networker.diffLessonsInCache(intervalToDiff, this)

        override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
            BugLogger.logBug("Something bad happened when diffing lessons", throwable)
            filterDiffAndDispatchCheckTerminated(false)
            return RetryConstraint.CANCEL
        }

        override fun onRequestCompleted() {
            numRequestsSucceeded++
            checkIfWeHaveFinished()
        }

        override fun onLoadingError() {
            numRequestsFailed++
            checkIfWeHaveFinished()
        }

        override fun onNumberOfRequestToSendKnown(numRequests: Int) {
            numRequestsSent = numRequests
        }

        fun checkIfWeHaveFinished() {
            if (numRequestsSent == numRequestsFailed + numRequestsSucceeded) {
                val allSucceeded = numRequestsSent == numRequestsSucceeded
                filterDiffAndDispatchCheckTerminated(allSucceeded)
            }
        }

        private fun filterDiffAndDispatchCheckTerminated(allSucceeded: Boolean) {
            diffAccumulator.discardPastLessons()
            onCheckTerminated.dispatch(diffAccumulator, allSucceeded)
        }

        override fun onNoLessonsInCache() = //Nothing to diff!
                filterDiffAndDispatchCheckTerminated(false)

        override fun onDiffResult(lessonsDiffResult: LessonsDiffResult) = diffAccumulator.addFrom(lessonsDiffResult)

        override fun onCancel(cancelReason: Int, throwable: Throwable?) = Unit

        override fun onAdded() = Unit
    }

    private inner class LoadMissingLessonsJob(private val intervalToLoad: WeekInterval) : Job(Params(1)) {

        internal val onCheckTerminated = Signal1<Boolean>()

        @Throws(Throwable::class)
        override fun onRun() = Networker.loadAndCacheNotCachedLessons(
                intervalToLoad, WaitForDownloadListenerToSignalAdapter(onCheckTerminated)
        )

        override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
            BugLogger.logBug("Something bad happened when loading missing lessons", throwable)
            onCheckTerminated.dispatch(false)
            return RetryConstraint.CANCEL
        }

        override fun onAdded() = Unit

        override fun onCancel(cancelReason: Int, throwable: Throwable?) = Unit
    }

    private interface LessonsDiffAndUpdateListener {
        fun onTerminated(successful: Boolean)
    }

    companion object {

        val EXTRA_STARTER = "EXTRA_STARTER"

        val STARTER_UNKNOWN = 0
        val STARTER_NETWORK_BROADCAST = 1
        val STARTER_BOOT_BROADCAST = 2
        val STARTER_APP_START = 3
        val STARTER_ALARM_MANAGER = 4
        val STARTER_SETTING_CHANGED = 5
        val STARTER_DEBUGGER = 6

        val SCHEDULE_SLOW = 1
        val SCHEDULE_QUICK = 2
        val SCHEDULE_MISSING = 3
        val NOTIFICATION_LESSONS_CHANGED_ID = 1000

        fun createIntent(context: Context, starter: Int): Intent {
            val intent = Intent(context, LessonsUpdaterService::class.java)
            intent.putExtra(EXTRA_STARTER, starter)
            return intent
        }

        fun cancelSchedules(context: Context, starter: Int) {
            val serviceIntent = LessonsUpdaterService.createIntent(
                    context, starter
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val pendingIntent = PendingIntent.getService(context,
                    0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            pendingIntent.cancel()
            alarmManager.cancel(pendingIntent)
        }
    }
}
