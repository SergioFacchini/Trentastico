package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import com.geridea.trentastico.utils.time.CalendarUtils
import java.util.*

class Semester(private val year: Int, private val semesterNumber: Int) {

    constructor(date: Calendar) : this(date.get(Calendar.YEAR), getSemesterNumber(date.get(Calendar.MONTH))) {}

    override fun equals(obj: Any?): Boolean {
        if (obj is Semester) {
            val anotherSemester = obj as Semester?
            return this.year == anotherSemester!!.year && this.semesterNumber == anotherSemester.semesterNumber
        }

        return false
    }

    private fun enclosesDate(date: Calendar): Boolean =
            year == date.get(Calendar.YEAR) && getSemesterNumber(date) == semesterNumber

    companion object {

        private val SEMESTER1_START = 9
        private val SEMESTER1_END = 1

        private val SEMESTER2_START = 2
        private val SEMESTER2_END = 8

        private val CURRENT_SEMESTER = Semester(CalendarUtils.debuggableToday)

        fun isInCurrentSemester(date: Calendar): Boolean = CURRENT_SEMESTER.enclosesDate(date)

        fun isInCurrentSemester(lesson: LessonSchedule): Boolean =
                isInCurrentSemester(lesson.startCal)

        fun getSemesterNumber(date: Calendar): Int = getSemesterNumber(date.get(Calendar.MONTH))

        fun getSemesterNumber(monthNumber: Int): Int =
                if (monthNumber in SEMESTER2_START..SEMESTER2_END) 2 else 1
    }
}
