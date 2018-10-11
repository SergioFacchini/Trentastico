package com.geridea.trentastico.logger


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import android.content.Context
import android.util.Log
import com.geridea.trentastico.model.ExtraCoursesList
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE
import com.hypertrack.hyperlog.HyperLog
import com.hypertrack.hyperlog.LogFormat
import com.threerings.signals.Signal1


object BugLogger {

    private const val TAG = "TRENTASTICO"

    val onNewDebugMessageArrived: Signal1<String> = Signal1()

    fun init(context: Context) {
        HyperLog.initialize(context, ReducedLogFormat(context))
        HyperLog.setLogLevel(Log.VERBOSE)
    }

    fun logBug(reason: String, e: Exception) {
        if (IS_IN_DEBUG_MODE) {
            return
        }

        HyperLog.e(TAG, reason, e)
        onNewDebugMessageArrived.dispatch(e.message!!)
    }

    fun setStudyCourse(course: StudyCourse) {
        HyperLog.d(TAG, "Study course changed to "+course.generateFullDescription())
    }

    fun setExtraCourses(extraCourses: ExtraCoursesList) {
        val courses = extraCourses.joinToString("\n") { it.fullName }
        HyperLog.d(TAG, "Extra study courses changed: $courses")
    }

    fun warn(message: String) {
        HyperLog.w(TAG, message)

        onNewDebugMessageArrived.dispatch(message)
    }

    fun info(message: String) {
        HyperLog.i(TAG, message)

        onNewDebugMessageArrived.dispatch(message)
    }


}

internal class ReducedLogFormat(context: Context) : LogFormat(context) {

    override fun getFormattedLogMessage(
            logLevelName: String, tag:        String, message:   String,
            timeStamp:    String, senderName: String, osVersion: String,
            deviceUUID:   String
    ): String = "$timeStamp : $logLevelName : $message"

}
