package com.geridea.trentastico.utils.time


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import java.util.*

class WeekTime {
    private var year: Int = 0
        private set
    private var weekNumber: Int = 0
        private set

    constructor(year: Int, weekNumber: Int) {
        this.year = year
        this.weekNumber = weekNumber
    }

    constructor(calendar: Calendar) {
        year       = calendar.get(Calendar.YEAR)
        weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * @return true if the current week time start after the argument.
     */
    fun after(arg: WeekTime): Boolean = if (this.year == arg.year) {
        this.weekNumber > arg.weekNumber
    } else
        this.year > arg.year

    /**
     * @return true if the current week time start before the argument.
     */
    fun before(arg: WeekTime): Boolean = if (this.year == arg.year) {
        this.weekNumber < arg.weekNumber
    } else
        this.year < arg.year

    fun hasSameWeekTime(day: WeekDayTime): Boolean = weekNumber == day.weekNumber && year == day.year

    fun before(day: WeekDayTime): Boolean = if (this.year == day.year) {
        this.weekNumber < day.weekNumber
    } else
        this.year < day.year

    fun after(day: WeekDayTime): Boolean = if (this.year == day.year) {
        this.weekNumber > day.weekNumber
    } else
        this.year > day.year


    fun addWeeks(numWeeksToAdd: Int): WeekTime {
        //Some years may have 53 weeks, so it's a good thing to delegate this calculation to the
        //calendar
        val dummy = CalendarUtils.clearCalendar
        dummy.set(Calendar.YEAR, year)
        dummy.set(Calendar.WEEK_OF_YEAR, weekNumber)
        dummy.add(Calendar.WEEK_OF_YEAR, numWeeksToAdd)

        year = dummy.get(Calendar.YEAR)
        weekNumber = dummy.get(Calendar.WEEK_OF_YEAR)

        return this
    }

    fun copy(): WeekTime = WeekTime(year, weekNumber)

    override fun equals(other: Any?): Boolean {
        if (other is WeekTime) {
            val time = other as WeekTime?
            return time!!.weekNumber == weekNumber && time.year == year
        }
        return false
    }

    /**
     * True if the current week time start before or equals the argument.
     */
    fun beforeOrEqual(weekTime: WeekTime): Boolean = before(weekTime) || equals(weekTime)

    override fun toString(): String = year.toString() + "/" + weekNumber

    override fun hashCode(): Int {
        var result = year
        result = 31 * result + weekNumber
        return result
    }

    val millis: Long
        get() {
            val calendar = CalendarUtils.clearCalendar
            calendar.set(Calendar.WEEK_OF_YEAR, weekNumber)
            calendar.set(Calendar.YEAR, year)

            return calendar.timeInMillis
        }
}
