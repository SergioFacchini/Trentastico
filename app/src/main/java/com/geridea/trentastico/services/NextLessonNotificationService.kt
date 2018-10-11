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
        val lessons = Networker.syncLoadTodaysCachedLessons()

        //Finding passed lessons to hide the notifications of the past lessons
        val passedLessons = findPassedLessons(lessons)
        val ongoingLessons = findOngoingLessons(lessons)

        //Hiding the notification for passed lessons
        passedLessons.forEach { id -> onLessonNotificationExpired.dispatch(id.hashCode()) }

        //Calculating lessons that could possibly be shown
        val validLessons = getShownLessons(lessons)
        validLessons.removeAll(passedLessons)

        if (validLessons.isEmpty()) {
            //We have no (more) lessons today. We'll schedule for tomorrow
            BugLogger.info("No more lessons today", "NLN")
            scheduleNextStartAt(calculateNextDayMorning())
        } else {
            //Finding the lessons starting in less than N minutes:
            val lessonsStartingSoon = findLessonsStartingSoon(validLessons)
            if (lessonsStartingSoon.isEmpty()) {
                //There are no lessons starting soon, however we still have some lessons to
                //show. We're going to show the notification for the lessons starting next
                val nextLessons = findLessonsStartingNext(validLessons)
                showNotificationsForLessonsIfNeeded(nextLessons)

                //Since we've already shown notifications for these lessons, we won't
                //consider them for the next scheduling:
                validLessons.removeAll(nextLessons)
                scheduleForTheNextLessonAndStop(validLessons, ongoingLessons)
            } else {
                //We have to show a notification for each lesson:
                showNotificationsForLessonsIfNeeded(lessonsStartingSoon)

                //We have already shown notifications for the lessons starting soon. We don't
                //consider these for the next start calculation
                validLessons.removeAll(lessonsStartingSoon)
                scheduleForTheNextLessonAndStop(validLessons, ongoingLessons)
            }
        }

        BugLogger.info("Next lesson notification ended...", "NLN")
        return Result.SUCCESS
    }


    private fun showNotificationsForLessonsIfNeeded(lessons: List<LessonSchedule>) =
            lessons.filter { notificationsTracker.shouldNotificationBeShown(it) }
                   .forEach { onLessonNotificationToShow.dispatch(it) }

    private fun findOngoingLessons(lessons: List<LessonSchedule>): ArrayList<LessonSchedule> {
        val now = CalendarUtils.debuggableMillis
        return lessons.filterTo(ArrayList()) { it.isHeldInMilliseconds(now) }
    }

    /**
     * Finds all the next lessons starting at the same time
     */
    private fun findLessonsStartingNext(lessons: List<LessonSchedule>): List<LessonSchedule> {
        //Here we suppose that the ordering of the lessons didn't change:
        val nextTimeLessonStating = lessons[0].startsAt
        return lessons.takeWhile { it.startsAt == nextTimeLessonStating }
    }

    private fun findPassedLessons(lessons: List<LessonSchedule>): ArrayList<LessonSchedule> {
        val now = CalendarUtils.debuggableMillis
        return lessons.filterTo(ArrayList()) { it.startsAt <= now }
    }

    private fun findLessonsStartingSoon(lessons: ArrayList<LessonSchedule>): ArrayList<LessonSchedule> {
        val millisSoon = CalendarUtils.getMillisWithMinutesDelta(Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN)
        return lessons.filterTo(ArrayList()) { it.startsBefore(millisSoon) }
    }

    private fun scheduleForTheNextLessonAndStop(
            notNotifiedLessons: List<LessonSchedule>,
            ongoingLessons: List<LessonSchedule>
    ) {
        //Note: If we're finishing a lesson and don't have the next lesson starting immediately at
        //the end of it, then we must show a notification before the end of that lessons telling
        //when the next lessons starts.

        //Finding when the next lessons start
        val nextStart  = notNotifiedLessons.minBy { it.startsAt }?.startsAt

        //Finding the end of the current lesson
        val ongoingEnd = ongoingLessons.minBy { it.endsAt }?.endsAt

        //Deciding when to plan the next schedule. There can be 4 situations:
        //* There is an ongoing lesson and another lesson after: min()-anticipation
        //* There are no ongoing lessons, and there is a lesson after: after next lesson
        //  (to hide the notification)
        //* There is an ongoing lesson, and there are no lessons after: end of the lesson
        //  (to hide the notification)
        //* There are no ongoing lessons, and there no lessons after: next morning
        val nextStartPlannedAt = when {
            nextStart != null && ongoingEnd != null -> {
                val next         = Math.min(nextStart, ongoingEnd)
                val anticipation = -Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN
                CalendarUtils.addMinutes(next, anticipation)
            }
            nextStart != null && ongoingEnd == null -> nextStart
            nextStart == null && ongoingEnd != null -> ongoingEnd
            nextStart == null && ongoingEnd == null -> calculateNextDayMorning()
            else -> {
                val runtimeException = RuntimeException("Should never happen")
                BugLogger.logBug("Should never happen", runtimeException, "NLN")
                throw runtimeException
            }
        }

        scheduleNextStartAt(nextStartPlannedAt)
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
            BugLogger.info("Forced next lesson notification start", "NLN")
            scheduleNextStartAt(System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(2))
        }

        fun scheduleNextStartAt(ms: Long, delta: Long = TimeUnit.MINUTES.toMillis(2)) {
            val windowStart = ms - System.currentTimeMillis() + 1
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
