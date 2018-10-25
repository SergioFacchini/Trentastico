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
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.geridea.trentastico.Config
import com.geridea.trentastico.Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN
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

class NextLessonNotificationService : Job() {

    private lateinit var notificationsTracker: ShownNotificationsTracker

    override fun onRunJob(params: Params): Result {
        BugLogger.info("Next lesson notification started...", "NLN")

        notificationsTracker = AppPreferences.notificationTracker

        //How this should work:
        //1) Always show notifications of the next lessons
        //2) Keep the notification shown until 15 minutes before the start of the next lesson. After
        //   that period show the notification for the next lesson.
        //3) If you've just finished the last, show until the end of the lesson +15 min that
        //   there are no more lessons today.
        //4) Reschedule the service to start 15 min before the start of the next lesson
        val loadResult = Networker.syncLoadTodaysLessons()

        //Considering only the shown lessons
        val lessons = getShownLessons(loadResult.lessons)

        //Hiding the notifications for passed lessons
        val passedLessons  = findPassedLessons(lessons)
        passedLessons.forEach { id -> onLessonNotificationExpired.dispatch(id.hashCode()) }

        //Finding the next starting lesson
        lessons.removeAll(passedLessons)
        lessons.sortBy { it.startsAt }
        val nextLessonStart = lessons.firstOrNull()?.startsAt

        if(nextLessonStart == null){
            //No more lessons today!
            BugLogger.info("No more lessons today", "NLN")

            notificationsTracker.clear()
            scheduleNextStartAt(calculateNextDayMorning())
        } else {
            //Finding the lessons that start next
            val nextStartingLessons = lessons.takeWhile { it.startsAt == nextLessonStart }
            showNotificationsForLessonsIfNeeded(nextStartingLessons)

            //Calculating all the remaining starting points. We have to look for the lessons
            //that still have to end or the ones that have to start. This algorithm works well even
            //in case of a lesson starting after the beginning of another lesson and finishing before
            //it.
            val startingPoints = mutableListOf<Long>()
            nextStartingLessons.forEach { startingPoints.add(it.endsAt) }

            lessons.forEach {
                startingPoints.add(it.endsAt)
            }
            startingPoints.sort()

            scheduleNextStartWithAnticipationAt(startingPoints.first())
        }


        BugLogger.info("Next lesson notification ended...", "NLN")
        return Result.SUCCESS
    }


    private fun showNotificationsForLessonsIfNeeded(lessons: List<LessonSchedule>) =
            lessons.filter { notificationsTracker.shouldNotificationBeShown(it) }
                   .forEach { onLessonNotificationToShow.dispatch(it) }

    private fun findPassedLessons(lessons: List<LessonSchedule>): ArrayList<LessonSchedule> {
        val now = CalendarUtils.debuggableMillis

        val anticipationMs = TimeUnit.MINUTES.toMillis(NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN.toLong())
        return lessons.filterTo(ArrayList()) { it.endsAt - anticipationMs <= now }
    }

    private fun calculateNextDayMorning(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        calendar.add(Calendar.DAY_OF_MONTH, 1)

        return calendar.timeInMillis
    }

    /**
     * @return lessons that are not hidden
     */
    private fun getShownLessons(lessons: List<LessonSchedule>): ArrayList<LessonSchedule> {
        val typesToHide = AppPreferences.lessonTypesToHideIds

        return lessons.filterNotTo(ArrayList()) { //Is it filtered by the lesson type or the lessons already passed?
            typesToHide.contains(it.lessonTypeId)
        }
    }

    companion object {

        const val TAG = "Next_Lesson_Notification_Service"

        private const val NOTIFICATION_CHANNEL_ID = "Prossima lezione"

        /**
         * Dispatched the id of a notification of a lesson that is no longer pertinent and should
         * be dismissed
         */
        val onLessonNotificationExpired: Signal1<Int> = Signal1()

        /**
         * Dispatched when a notification for a lesson has to be shown
         */
        val onLessonNotificationToShow: Signal1<LessonSchedule> = Signal1()


        fun clearNotifications(context: Context) = //Technically this could cancel the "Your lessons has changed" notification. However, this
                //would happen very infrequently since the changes to lesson are not performed very often
                getNotificationManager(context).cancelAll()

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
                    .setColor(context.resources.getColor(R.color.colorNotification))

            if (AppPreferences.nextLessonNotificationsFixed) {
                notificationBuilder.setOngoing(true)
            }

            //Appending an intent to the notification
            val intent = Intent(context, FirstActivityChooserActivity::class.java)
            notificationBuilder.setContentIntent(
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            )

            //Launching the notification
            val notificationId = lesson.id.hashCode()

            val manager = getNotificationManager(context)
            manager.notify(notificationId, notificationBuilder.build())

            AppPreferences.notificationTracker.notifyNotificationShown(notificationId)
        }

        fun scheduleNow() {
            if (AppPreferences.isStudyCourseSet) {
                BugLogger.info("Forced next lesson notification start", "NLN")
                scheduleNextStartAt(System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(2))
            }
        }

        fun scheduleNextStartWithAnticipationAt(ms: Long) {
            val anticipation = -Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN
            val anticipatedMs = CalendarUtils.addMinutes(ms, anticipation)

            scheduleNextStartAt(anticipatedMs)
        }

        fun scheduleNextStartAt(ms: Long, delta: Long = TimeUnit.MINUTES.toMillis(1)) {
            val windowStart = Math.max(ms - System.currentTimeMillis(), 0) + 1
            val windowEnd   = windowStart + delta

            JobRequest.Builder(TAG)
                    .setExecutionWindow(windowStart, windowEnd)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()

            val message = "Next start scheduled to ${CalendarUtils.formatTimestamp(ms)}"
            BugLogger.info(message, "NLN")
        }

    }

}
