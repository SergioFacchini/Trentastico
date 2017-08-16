package com.geridea.trentastico.utils.time


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import java.util.Calendar

class WeekDayTime(var year: Int, var weekNumber: Int, private val weekDay: Int) {

    @JvmOverloads constructor(calendar: Calendar = CalendarUtils.debuggableToday) : this(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.WEEK_OF_YEAR),
            calendar.get(Calendar.DAY_OF_WEEK)
    ) {
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is WeekDayTime) {
            val anotherTime = obj as WeekDayTime?
            return this.year == anotherTime!!.year &&
                    this.weekNumber == anotherTime.weekNumber &&
                    this.weekDay == anotherTime.weekDay
        }

        return false
    }

    fun before(anotherDay: WeekDayTime): Boolean {
        if (this.year < anotherDay.year) {
            return true
        } else if (this.year > anotherDay.year) {
            return false
        }

        if (this.weekNumber < anotherDay.weekNumber) {
            return true
        } else if (this.weekNumber > anotherDay.weekNumber) {
            return false
        }

        return if (this.weekDay < anotherDay.weekDay) {
            true
        } else if (this.weekDay > anotherDay.weekDay) {
            false
        } else {
            false
        }

    }

    /**
     * @return the smallest week interval that contains this WeekDayTime.
     */
    val containingInterval: WeekInterval
        get() {
            val from = WeekTime(this)
            val to = from.copy().addWeeks(1)

            return WeekInterval(from, to)
        }
}
