package com.geridea.trentastico.logger


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import com.geridea.trentastico.model.ExtraCoursesList
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE

//TODO: reenable ACRA
object BugLogger {

    fun logBug(reason: String, e: Throwable) {
        if (IS_IN_DEBUG_MODE) {
            return
        }

        //errorReporter.putCustomData("REPORT-REASON", reason)
        //errorReporter.handleSilentException(e)
    }

    fun setStudyCourse(course: StudyCourse) {
        if (IS_IN_DEBUG_MODE) {
            return
        }

        //errorReporter.putCustomData("STUDY-COURSE", course.toString())
    }

    fun setExtraCourses(extraCourses: ExtraCoursesList) {
        if (IS_IN_DEBUG_MODE) {
            return
        }

        //errorReporter.putCustomData("EXTRA-COURSES", extraCourses.toJSON().toString())
    }

    //private val errorReporter: ErrorReporter
    //    get() = ACRA.getErrorReporter()

    fun init() {
        if (IS_IN_DEBUG_MODE) {
            return
        }

        //errorReporter.putCustomData("android-id", AppPreferences.androidId)
        if (AppPreferences.isStudyCourseSet) {
            setStudyCourse(AppPreferences.studyCourse)
        }
        setExtraCourses(AppPreferences.extraCourses)
    }

}
