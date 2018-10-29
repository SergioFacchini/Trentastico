package com.geridea.trentastico.services

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.geridea.trentastico.Config
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarUtils
import com.threerings.signals.Signal1
import java.util.concurrent.TimeUnit


/*
 * Created with ♥ by Slava on 29/10/2018.
 */
class NextLessonNotificationHideService : Job() {

    override fun onRunJob(params: Params): Result {
        //We just find the notifications of the lessons that already ended and we hide them
        //This procedure can call multiple time to close the same notification, but in that case
        //nothing will happen, so we continue doing it
        val tracker = AppPreferences.notificationTracker
        val idsToHide = tracker.getShownNotificationIds()

        idsToHide.forEach {
            onLessonNotificationExpired.dispatch(it)
        }

        return Result.SUCCESS
    }

    companion object {

        const val TAG = "Next_Lesson_Notification_Hide_Service"

        /**
         * Dispatched the id of a notification of a lesson that is no longer pertinent and should
         * be dismissed
         */
        val onLessonNotificationExpired: Signal1<Int> = Signal1()

        fun scheduleWithAnticipationAt(ms: Long, delta: Long = TimeUnit.MINUTES.toMillis(1)) {
            val anticipationMs = TimeUnit.MINUTES.toMillis(Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN.toLong())

            val windowStart = Math.max(ms - System.currentTimeMillis() - anticipationMs, 0) + 1
            val windowEnd   = windowStart + delta

            JobRequest.Builder(TAG)
                    .setExecutionWindow(windowStart, windowEnd)
                    .build()
                    .schedule()

            val message = "Hide notification scheduled to ${CalendarUtils.formatTimestamp(ms)}"
            BugLogger.info(message, "NLNH")
        }

    }

}