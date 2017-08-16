package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 04/04/2017.
 */

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.geridea.trentastico.Config
import com.geridea.trentastico.R
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.gui.activities.FirstActivityChooserActivity
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.LessonsWithRoomListener
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ArrayUtils
import com.geridea.trentastico.utils.UIUtils.showToastIfInDebug
import com.geridea.trentastico.utils.time.CalendarUtils
import java.util.*

class NextLessonNotificationService : Service() {

    private lateinit var notificationsTracker: ShownNotificationsTracker

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        notificationsTracker = AppPreferences.notificationTracker
    }

    override fun onDestroy() {
        super.onDestroy()

        AppPreferences.notificationTracker = notificationsTracker
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val starter = intent.getSerializableExtra(EXTRA_STARTER) as NLNStarter

        if (ArrayUtils.isOneOf(starter, NLNStarter.ALARM_MORNING, NLNStarter.STUDY_COURSE_CHANGE)) {
            //We're at the morning of the day. No notifications of the day shown so far. We can
            //clear the tracker.
            notificationsTracker.clear()
        }

        if (!AppPreferences.isStudyCourseSet) {
            //Probably first start. We have nothing to show to a user that doesn't have any lesson
            //planned!
            stopSelf()
            return Service.START_NOT_STICKY
        }

        if (!AppPreferences.areNextLessonNotificationsEnabled()) {
            //The notifications are disabled: nothing to do!
            stopSelf()
            return Service.START_NOT_STICKY
        }

        //How this should work:
        //1) Always show notifications of the next lessons
        //2) Keep the notification shown until 15 minutes before the start of the next lesson. After
        //   that period show the notification for the next lesson.
        //3) If you've just finished the last, show until the end of the lesson +15 min that
        //   there are no more lessons today.
        //4) Reschedule the service to start 15 min before the start of the next lesson
        Networker.loadTodaysLessons(object : TodaysLessonsListener {

            override fun onLessonsAvailable(lessons: ArrayList<LessonSchedule>) {
                //Finding passed lessons to hide the notifications of the past lessons
                val passedLessons = findPassedLessons(lessons)
                val currentLessons = findCurrentLessons(lessons)

                hideNotificationsForPassedLessons(passedLessons)

                //Calculating lessons that will possibly be shown
                val validLessons = getValidLessons(lessons)
                validLessons.removeAll(passedLessons)

                if (validLessons.isEmpty()) {
                    //We have no (more) lessons today. We'll schedule for tomorrow
                    showToastIfInDebug(this@NextLessonNotificationService, "No lessons for today!")
                    scheduleAtNextDayMorning()
                } else {
                    //Finding the lessons starting in less than N minutes:
                    val lessonsStartingSoon = findLessonsStartingSoon(validLessons)
                    if (lessonsStartingSoon.isEmpty()) {
                        //There are no lessons starting soon, however we still have some lessons to
                        //show. We're going to show the notification for the lessons starting next
                        val nextLessons = findLessonsStartingNext(validLessons)
                        loadRoomsForLessonsIfMissing(nextLessons, object: LessonsWithRoomListener {
                            override fun onLoadingCompleted(
                                    updatedLessons: ArrayList<LessonSchedule>,
                                    lessonsWithoutRooms: ArrayList<LessonSchedule>) {

                                showNotificationsForLessonsIfNeeded(updatedLessons, starter)

                                notificationsTracker.notifyLessonsWithoutRoom(lessonsWithoutRooms)

                                //Since we've already shown notifications for these lessons, we won't
                                //consider them for the next scheduling:
                                validLessons.removeAll(updatedLessons)
                                scheduleForTheNextLessonAndStop(validLessons, currentLessons)
                            }
                        })

                    } else {
                        loadRoomsForLessonsIfMissing(lessonsStartingSoon, object: LessonsWithRoomListener {
                            override fun onLoadingCompleted(
                                    updatedLessons: ArrayList<LessonSchedule>,
                                    lessonsWithoutRooms: ArrayList<LessonSchedule>) {

                                //We have to show a notification for each lesson:
                                showNotificationsForLessonsIfNeeded(updatedLessons, starter)

                                notificationsTracker.notifyLessonsWithoutRoom(lessonsWithoutRooms)

                                //We have already shown notifications for the lessons starting soon. We don't
                                //consider these for the next start calculation
                                validLessons.removeAll(updatedLessons)
                                scheduleForTheNextLessonAndStop(validLessons, currentLessons)
                            }
                        })

                    }
                }
            }

            override fun onLessonsCouldNotBeLoaded() {
                //We're going to wait for the lessons to be available; the service will still start
                //at the app opening and network state change.
                stopSelf()
            }
        })

        return Service.START_NOT_STICKY
    }

    private fun showNotificationsForLessonsIfNeeded(lessons: ArrayList<LessonSchedule>, starter: NLNStarter) {
        lessons.filter { notificationsTracker.shouldNotificationBeShown(it, starter) }
               .forEach { showNotificationForLessons(it) }
    }

    private fun loadRoomsForLessonsIfMissing(lessons: ArrayList<LessonSchedule>, listener: LessonsWithRoomListener) {
        Networker.loadRoomsForLessonsIfMissing(lessons, listener)
    }

    private fun findCurrentLessons(lessons: ArrayList<LessonSchedule>): ArrayList<LessonSchedule> {
        val now = CalendarUtils.debuggableMillis

        val currentLessons = ArrayList<LessonSchedule>()
        for (lesson in lessons) {
            if (lesson.isHeldInMilliseconds(now)) {
                currentLessons.add(lesson)
            }
        }
        return currentLessons
    }

    /**
     * Finds all the next lessons starting at the same time
     */
    private fun findLessonsStartingNext(lessons: ArrayList<LessonSchedule>): ArrayList<LessonSchedule> {
        val lessonsStartingNext = ArrayList<LessonSchedule>()

        //Here we suppose that the ordering of the lessons didn't change:
        val nextTimeLessonStating = lessons[0].startsAt
        for (lesson in lessons) {
            if (lesson.startsAt == nextTimeLessonStating) {
                lessonsStartingNext.add(lesson)
            }
        }

        return lessonsStartingNext
    }

    private fun hideNotificationsForPassedLessons(passedLessons: ArrayList<LessonSchedule>) {
        val manager = getNotificationManager(this@NextLessonNotificationService)

        for (lesson in passedLessons) {
            manager.cancel(lesson.id.toInt())
        }
    }

    private fun findPassedLessons(lessons: ArrayList<LessonSchedule>): ArrayList<LessonSchedule> {
        val passedLessons = ArrayList<LessonSchedule>()

        val now = CalendarUtils.debuggableMillis
        for (lesson in lessons) {
            if (lesson.startsAt <= now) {
                passedLessons.add(lesson)
            }
        }
        return passedLessons
    }

    private fun findLessonsStartingSoon(lessons: ArrayList<LessonSchedule>): ArrayList<LessonSchedule> {
        val millisSoon = CalendarUtils.getMillisWithMinutesDelta(Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN)

        val lessonsStartingSoon = ArrayList<LessonSchedule>()
        for (lesson in lessons) {
            if (lesson.startsBefore(millisSoon)) {
                lessonsStartingSoon.add(lesson)
            }
        }
        return lessonsStartingSoon
    }

    private fun scheduleForTheNextLessonAndStop(lessons: ArrayList<LessonSchedule>, currentLessons: ArrayList<LessonSchedule>) {
        //Note: If we're finishing a lesson and don't have the next lesson starting immediately at
        // the end of it, then we must show a notification before the end of that lessons telling us
        // when the next lessons starts.

        //Finding when the next lessons start
        var nextLessonStart: Long? = null
        for (lesson in lessons) {
            if (nextLessonStart == null || lesson.startsAt < nextLessonStart) {
                nextLessonStart = lesson.startsAt
            }
        }

        //Finding the end of the current lessons
        var currentLessonEnd: Long? = null
        for (lesson in currentLessons) {
            if (currentLessonEnd == null || lesson.endsAt < currentLessonEnd) {
                currentLessonEnd = lesson.endsAt
            }
        }

        //Deciding when to plan the next schedule
        val nextStartPlannedAt: Long?
        nextStartPlannedAt = if (nextLessonStart != null && currentLessonEnd != null) {
            Math.min(nextLessonStart, currentLessonEnd)
        } else if (nextLessonStart == null && currentLessonEnd == null) {
            null
        } else {
            if (nextLessonStart != null) nextLessonStart else currentLessonEnd
        }

        if (nextStartPlannedAt == null) {
            scheduleAtNextDayMorning()
        } else {
            val ms = CalendarUtils.addMinutes(nextStartPlannedAt, -Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN)
            scheduleNextStartAt(ms, false)
        }

        stopSelf()
    }

    private fun scheduleAtNextDayMorning() {
        scheduleNextStartAt(nextDayMorning, true)
    }

    private val nextDayMorning: Long
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            calendar.add(Calendar.DAY_OF_MONTH, 1)

            return calendar.timeInMillis
        }

    private fun scheduleNextStartAt(ms: Long, isScheduledAtMorning: Boolean) {
        val starter = if (isScheduledAtMorning) NLNStarter.ALARM_MORNING else NLNStarter.ALARM_LESSON
        val serviceIntent = createIntent(this, starter)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.set(AlarmManager.RTC, ms, pendingIntent)

        showToastIfInDebug(this, "Scheduled alarm manager to " + CalendarUtils.formatTimestamp(ms))
    }

    private fun showNotificationForLessons(lesson: LessonSchedule) {
        val notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(lesson.subject)
                .setContentText(lesson.synopsis)
                .setColor(resources.getColor(R.color.colorNotification))
                .setAutoCancel(true)

        if (AppPreferences.areNextLessonNotificationsFixed()) {
            notificationBuilder.setOngoing(true)
        }


        val intent = Intent(this, FirstActivityChooserActivity::class.java)

        val resultPendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationBuilder.setContentIntent(resultPendingIntent)

        val manager = getNotificationManager(this@NextLessonNotificationService)
        manager.notify(lesson.id.toInt(), notificationBuilder.build())

        notificationsTracker!!.notifyNotificationShown(lesson.id)
    }

    private fun getValidLessons(lessons: ArrayList<LessonSchedule>): ArrayList<LessonSchedule> {
        val lessonsToKeep = ArrayList<LessonSchedule>()
        val typesToHide = AppPreferences.lessonTypesIdsToHide

        for (lesson in lessons) {
            //Is it filtered by the lesson type or the lessons already passed?
            if (!typesToHide.contains(lesson.lessonTypeId)) {
                //Checking if we're filtering this lessons because of partitionings
                if (!isFilteredByPartitionings(lesson)) {
                    lessonsToKeep.add(lesson)
                }
            }

        }

        return lessonsToKeep
    }

    private fun isFilteredByPartitionings(lesson: LessonSchedule): Boolean =
            AppPreferences.getHiddenPartitionings(lesson.lessonTypeId).any { lesson.hasPartitioning(it) }

    companion object {

        val EXTRA_STARTER = "EXTRA_STARTER"

        fun createIntent(context: Context, starter: NLNStarter): Intent {
            val intent = Intent(context, NextLessonNotificationService::class.java)
            intent.putExtra(EXTRA_STARTER, starter)
            return intent
        }

        fun clearNotifications(context: Context) {
            //Technically this could cancel the "Your lessons has changed" notification. However, this
            //would happen very infrequently since the changes to lesson are not performed very often
            getNotificationManager(context).cancelAll()


        }

        private fun getNotificationManager(context: Context): NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /**
         * Removes all notification related to the passed course. This happens by retrieving all the
         * lessons held today and removing all the possible notifications of that type.
         * @param context needed to invoke the notification service
         * @param course the course
         */
        fun removeNotificationsOfExtraCourse(context: Context, course: ExtraCourse) {
            Networker.getTodaysCachedLessons(object : TodaysLessonsListener {
                override fun onLessonsAvailable(lessons: ArrayList<LessonSchedule>) {
                    val notificationManager = getNotificationManager(context)

                    for (lesson in lessons) {
                        if (course.isLessonOfCourse(lesson)) {
                            notificationManager.cancel(lesson.id.toInt())
                        }
                    }
                }

                override fun onLessonsCouldNotBeLoaded() { /* Not used in this case*/
                }
            })

        }
    }

}
