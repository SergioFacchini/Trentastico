package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.geridea.trentastico.Config
import com.geridea.trentastico.Config.LESSONS_REFRESH_WAITING_REGULAR
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.LessonsChangedActivity
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.DiffLessonsListener
import com.geridea.trentastico.network.request.LessonsDiffResult
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarUtils
import com.threerings.signals.Signal2
import java.util.concurrent.TimeUnit


class LessonsUpdaterJob : Job() {

    private var numRequestsToSend   = 0
    private var numRequestsFinished = 0

    override fun onRunJob(params: Params): Result {
        BugLogger.info("Lesson updater job is running...", "LESSON_UPDATER")

        //Performing the diff
        numRequestsToSend = AppPreferences.extraCourses.size + 1

        //Study course
        val courseName = AppPreferences.studyCourse.courseName
        val lastValidTimestamp = CalendarUtils.debuggableMillis + Config.LESSONS_CHANGED_ANTICIPATION_MS
        Networker.syncDiffStudyCourseLessonsWithCachedOnes(
                lastValidTimestamp, getDiffLessonsListener(courseName)
        )

        //Extra courses
        AppPreferences.extraCourses.forEach {
            Networker.diffExtraCourseLessonsWithCachedOnes(
                    it, lastValidTimestamp, getDiffLessonsListener(it.lessonName)
            )
        }

        BugLogger.info("Lesson updater job has finished...", "LESSON_UPDATER")
        if (!params.isPeriodic) {
            //This task is not periodic only if it has been run from debug or the user has changed
            //the settings about lesson updates. Running the task in not-periodic way, stops it
            //from running periodically.
            //We have to resume the periodic running after the debug run
            schedulePeriodicRun()
        }

        return Result.SUCCESS
    }

    private fun getDiffLessonsListener(courseName: String): DiffLessonsListener {
        return object : DiffLessonsListener {

            override fun onBeforeRequestFinished() {
                numRequestsFinished++
            }

            override fun onLessonsDiffed(result: LessonsDiffResult) {
                if (result.hasSomeDifferences) {
                    onLessonsChanged.dispatch(result, courseName)
                }
            }

            override fun onLessonsLoadingError() {}

            override fun onNoCachedLessons() {}
        }
    }

    companion object {

        private const val NOTIFICATION_LESSONS_CHANGED_ID: Int = 1234

        const val TAG = "Update_Lessons_Job"
        private const val NOTIFICATION_CHANNEL_ID = "Lezioni cambiate"

        val onLessonsChanged = Signal2<LessonsDiffResult, String>()

        fun schedulePeriodicRun() {
            if (AppPreferences.isSearchForLessonChangesEnabled && AppPreferences.isStudyCourseSet) {
                buildJob(false).schedule()
                BugLogger.info("Lesson updater scheduled...", "LESSON_UPDATER")
            }
        }

        fun cancelPeriodicRun() {
            JobManager.instance().cancelAllForTag(TAG)
            BugLogger.info("Lesson updater cancelled...", "LESSON_UPDATER")
        }

        fun runNowAndSchedulePeriodic() {
            if (AppPreferences.isSearchForLessonChangesEnabled && AppPreferences.isStudyCourseSet) {
                BugLogger.info("Lesson updater single run...", "LESSON_UPDATER")
                JobManager.instance().cancelAllForTag(TAG)
                buildJob(true).schedule()
            }
        }

        private fun buildJob(runNow: Boolean): JobRequest {
            val builder = JobRequest.Builder(TAG)
            return if(runNow) {
                builder.setExecutionWindow(
                        TimeUnit.SECONDS.toMillis(1),
                        TimeUnit.SECONDS.toMillis(2)
                )
            } else {
                builder
                    .setPeriodic(TimeUnit.HOURS.toMillis(LESSONS_REFRESH_WAITING_REGULAR))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setRequirementsEnforced(true)
            }.build()
        }

        fun showLessonsChangedNotification(context: Context, diffResult: LessonsDiffResult, courseName: String) {
            if (!AppPreferences.isNotificationForLessonChangesEnabled) {
                return
            }

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the notification channel for android O
                buildNotificationChannel(notificationManager)
            }

            val intent = Intent(context, LessonsChangedActivity::class.java)
            intent.putExtra(LessonsChangedActivity.EXTRA_DIFF_RESULT, diffResult)

            val pending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
            }
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(when (diffResult.numTotalDifferences) {
                        1 -> "È cambiato l'orario di una lezione!"
                        else -> "Sono cambiati gli orari di ${diffResult.numTotalDifferences} lezioni!"
                    })
                    .setContentText(courseName)
                    .setColor(context.resources.getColor(R.color.colorNotification))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pending)

            notificationManager.notify(NOTIFICATION_LESSONS_CHANGED_ID, NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).build())
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun buildNotificationChannel(notificationManager: NotificationManager) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, importance)
            channel.description = "Mostra una notifica quando qualche lezione è cambiata"

            notificationManager.createNotificationChannel(channel)
        }

    }
}
