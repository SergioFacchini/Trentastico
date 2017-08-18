package com.geridea.trentastico.utils.time


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import com.geridea.trentastico.utils.time.CalendarUtils.debuggableToday
import java.util.*

class WeekTime {
    var year: Int = 0
        private set
    var weekNumber: Int = 0
        private set

    constructor(year: Int, weekNumber: Int) {
        this.year = year
        this.weekNumber = weekNumber
    }

    constructor() {
        initFromCalendar(debuggableToday)
    }

    constructor(calendar: Calendar) {
        initFromCalendar(calendar)
    }

    constructor(milliseconds: Long) {
        val calendar = CalendarUtils.clearCalendar
        calendar.timeInMillis = milliseconds

        initFromCalendar(calendar)
    }

    constructor(weekDayTime: WeekDayTime) : this(weekDayTime.year, weekDayTime.weekNumber) {}

    fun initFromCalendar(calendar: Calendar) {
        year = calendar.get(Calendar.YEAR)
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

        initFromCalendar(dummy)

        return this
    }

    fun copy(): WeekTime = WeekTime(year, weekNumber)

    override fun equals(obj: Any?): Boolean {
        if (obj is WeekTime) {
            val time = obj as WeekTime?
            return time!!.weekNumber == weekNumber && time.year == year
        }
        return false
    }

    /**
     * True if the current week time start before or equals the argument.
     */
    fun beforeOrEqual(weekTime: WeekTime): Boolean = before(weekTime) || equals(weekTime)

    /**
     * True if the current week time start after or equals the argument.
     */
    fun afterOrEqual(weekTime: WeekTime): Boolean = after(weekTime) || equals(weekTime)

    override fun toString(): String = year.toString() + "/" + weekNumber

    override fun hashCode(): Int {
        var result = year
        result = 31 * result + weekNumber
        return result
    }
}
