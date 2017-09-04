package com.geridea.trentastico.utils.time


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import java.util.*

class WeekDayTime(var year: Int, var weekNumber: Int, private val weekDay: Int) {

    @JvmOverloads constructor(calendar: Calendar = CalendarUtils.debuggableToday) : this(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.WEEK_OF_YEAR),
            calendar.get(Calendar.DAY_OF_WEEK)
    )


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

        return when {
            this.weekDay < anotherDay.weekDay -> true
            this.weekDay > anotherDay.weekDay -> false
            else                              -> false
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeekDayTime

        if (year       != other.year)       return false
        if (weekNumber != other.weekNumber) return false
        if (weekDay    != other.weekDay)    return false

        return true
    }

    override fun hashCode(): Int {
        var result = year
        result = 31 * result + weekNumber
        result = 31 * result + weekDay
        return result
    }

}
