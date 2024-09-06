package com.geridea.trentastico.logger


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import android.content.Context
import com.geridea.trentastico.model.ExtraCoursesList
import com.geridea.trentastico.model.StudyCourse
import com.threerings.signals.Signal1


object BugLogger {

    private const val TAG = "TRENTASTICO"

    val onNewDebugMessageArrived: Signal1<String> = Signal1()

    fun init(context: Context) {
    }

    fun logBug(reason: String, e: Exception?, tag: String = TAG) {
        onNewDebugMessageArrived.dispatch(e?.message!!)
    }

    fun setStudyCourse(course: StudyCourse) {
    }

    fun setExtraCourses(extraCourses: ExtraCoursesList) {
        val courses = extraCourses.joinToString("\n") { it.fullName }
    }

    fun warn(message: String, tag: String = TAG) {

        onNewDebugMessageArrived.dispatch(message)
    }

    fun info(message: String, tag: String = TAG) {

        onNewDebugMessageArrived.dispatch(message)
    }

}
