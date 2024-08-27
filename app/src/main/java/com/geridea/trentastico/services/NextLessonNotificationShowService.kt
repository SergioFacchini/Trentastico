package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 04/04/2017.
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.res.ResourcesCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.geridea.trentastico.Config
import com.geridea.trentastico.Config.NEXT_LESSON_LOADING_WAIT_BETWEEN_ERRORS
import com.geridea.trentastico.R
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.gui.activities.FirstActivityChooserActivity
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarUtils
import com.threerings.signals.Signal1
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class NextLessonNotificationShowService : Job() {

    override fun onRunJob(params: Params): Result {
        BugLogger.info("Next lesson notification show started...", "NLNS")
        //1) Find the next lessons
        //2a) If there are lessons, show the notification for the lessons starting next
        //2b) Schedule lesson notification hider at the end of the lesson
        //2c) Schedule the next lesson notification at the min of the end of this lesson and the
        //    start of the next one
        //3) If there are no lessons, reschedule for the next day
        val loadResult = Networker.syncLoadTodaysLessons()
        if(loadResult.wereErrors){
            BugLogger.info("Error while loading lessons...", "NLNS")
            //There was an error in loading lessons. Continuing here is meaningless
            scheduleNextStartAt(calculateNextRunInCaseOfError())
            return Result.FAILURE
        }

        val lessons = getFutureVisibleLessons(loadResult.lessons)
        lessons.sortBy { it.startsAt }

        val nextLessonStart = lessons.firstOrNull()?.startsAt
        if(nextLessonStart != null){
            //There could be more than one lesson starting at the same time. We have to show a
            //notification for that one too.
            val nextStartingLessons = lessons.takeWhile { it.startsAt == nextLessonStart }
            showNotificationAndScheduleRemoval(nextStartingLessons)

            val remainingLessons = lessons.filterNot { it in nextStartingLessons }

            val theLessonStartingFirst = remainingLessons.firstOrNull()?.startsAt ?: Long.MAX_VALUE
            val theLessonEndingFirst = nextStartingLessons.minBy { it.endsAt }!!.endsAt

            val nextStart = Math.min(theLessonStartingFirst, theLessonEndingFirst)
            scheduleNextStartWithAnticipationAt(nextStart)
        } else {
            BugLogger.info("No more lessons today", "NLNS")

            //Not sure if we need this, but I think that sometimes the notification can hang on so
            //it's worth keeping
            clearNotifications(context)

            scheduleNextStartAt(calculateNextDayMorning())
        }

        BugLogger.info("Next lesson notification ended...", "NLNS")
        return Result.SUCCESS
    }


    private fun showNotificationAndScheduleRemoval(lessons: List<LessonSchedule>) =
            lessons.filter { AppPreferences.notificationTracker.shouldNotificationBeShown(it) }
                   .forEach {
                       onLessonNotificationToShow.dispatch(it)
                       AppPreferences.notificationTracker.notifyNotificationShown(it)
                       NextLessonNotificationHideService.scheduleWithAnticipationAt(it.endsAt)
                   }

    private fun calculateNextDayMorning(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        calendar.add(Calendar.DAY_OF_MONTH, 1)

        return calendar.timeInMillis
    }

    private fun calculateNextRunInCaseOfError(): Long =
            CalendarUtils.addHours(System.currentTimeMillis(), NEXT_LESSON_LOADING_WAIT_BETWEEN_ERRORS)

    /**
     * @return lessons that are not hidden and scheduled to start in the future
     */
    private fun getFutureVisibleLessons(lessons: List<LessonSchedule>): ArrayList<LessonSchedule> {
        val typesToHide = AppPreferences.lessonTypesToHideIds

        return lessons.filterTo(ArrayList()) { //Is it filtered by the lesson type or the lessons already passed?
            !typesToHide.contains(it.lessonTypeId) && it.startsAt >= System.currentTimeMillis()
        }
    }

    companion object {

        const val TAG = "Next_Lesson_Notification_Show_Service"

        private const val NOTIFICATION_CHANNEL_ID = "Prossima lezione"

        /**
         * Dispatched when a notification for a lesson has to be shown
         */
        val onLessonNotificationToShow: Signal1<LessonSchedule> = Signal1()


        fun clearNotifications(context: Context, clearAlreadyShownNofication: Boolean = true) {
            //Technically this could cancel the "Your lessons has changed" notification. However, this
            //would happen very infrequently since the changes to lesson are not performed very often
            getNotificationManager(context).cancelAll()

            if (clearAlreadyShownNofication) {
                AppPreferences.notificationTracker.clear()
            }
        }

        fun clearNotificationWithId(context: Context, id: Int) =
                getNotificationManager(context).cancel(id)

        private fun getNotificationManager(context: Context): NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /**
         * Removes all notification related to the passed course. This happens by retrieving all the
         * lessons held today and removing all the possible notifications of that type.
         * @param context needed to invoke the notification service
         * @param course the course
         */
        fun removeNotificationsOfExtraCourse(context: Context, course: ExtraCourse) = Networker.loadTodaysCachedLessons(object : TodaysLessonsListener {
            override fun onLessonsAvailable(lessons: List<LessonSchedule>) {
                val notificationManager = getNotificationManager(context)

                lessons.filter { course.isLessonOfCourse(it) }
                       .forEach { notificationManager.cancel(it.id.toInt()) }
            }
        })

        @RequiresApi(Build.VERSION_CODES.O)
        private fun buildNotificationChannel(notificationManager: NotificationManager) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel    = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, importance)
            channel.description = "Mostra una notifica con la prossima lezione"

            notificationManager.createNotificationChannel(channel)
        }

        fun showNotificationForLessons(context: Context, lesson: LessonSchedule) {
            //Creating notification channel
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                buildNotificationChannel(notificationManager)
            }

            //Creating notification
            val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(lesson.subject)
                    .setContentText(lesson.synopsis)
                    .setColor(ResourcesCompat.getColor(context.resources, R.color.colorNotification, null))


            if (AppPreferences.nextLessonNotificationsFixed) {
                notificationBuilder.setOngoing(true)
            }

            //Appending an intent to the notification
            val intent = Intent(context, FirstActivityChooserActivity::class.java)

            val pending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            notificationBuilder.setContentIntent(pending)

            //Launching the notification
            val notificationId = lesson.id.hashCode()

            val manager = getNotificationManager(context)
            manager.notify(notificationId, notificationBuilder.build())
        }

        fun scheduleNowIfEnabled() {
            if (AppPreferences.isStudyCourseSet && AppPreferences.nextLessonNotificationsEnabled) {
                BugLogger.info("Forced next lesson notification start", "NLNS")
                scheduleNextStartAt(System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(2))
            }
        }

        fun cancelScheduling() {
            //This will make sure that the notification of the day are re-shown in case the user
            //re-enables them
            AppPreferences.notificationTracker.clear()

            BugLogger.info("Canceled the next lesson notification scheduling", "NLNS")
            JobManager.instance().cancelAllForTag(TAG)
        }

        fun scheduleNextStartWithAnticipationAt(ms: Long) {
            val anticipation = -Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN
            val anticipatedMs = CalendarUtils.addMinutes(ms, anticipation)

            scheduleNextStartAt(anticipatedMs)
        }

        fun scheduleNextStartAt(ms: Long,
                                delta: Long = TimeUnit.MINUTES.toMillis(1)) {

            val windowStart = Math.max(ms - System.currentTimeMillis(), 0) + 1
            val windowEnd   = windowStart + delta

            JobRequest.Builder(TAG)
                    .setExecutionWindow(windowStart, windowEnd)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()

            val message = "Next start scheduled to ${CalendarUtils.formatTimestamp(ms)}"
            BugLogger.info(message, "NLNS")
        }

    }

}
