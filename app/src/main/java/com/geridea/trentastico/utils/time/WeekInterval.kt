package com.geridea.trentastico.utils.time


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger
import java.util.*

open class WeekInterval {

    private lateinit var start: WeekTime
    private lateinit var end:   WeekTime

    constructor(start: WeekTime, end: WeekTime) {
        init(start, end)
    }

    private fun init(start: WeekTime, end: WeekTime) {
        if (start.after(end)) {
            val e = IllegalStateException("Cannot have an interval starting after it's end!")
            BugLogger.logBug("Got an interval starting before it's end: $start---$end", e)
            throw e
        }

        this.start = start
        this.end = end
    }

    constructor(fromWhen: Calendar, toWhen: Calendar) : this(WeekTime(fromWhen), WeekTime(toWhen).addWeeks(1))


    private operator fun contains(day: WeekDayTime): Boolean = start.hasSameWeekTime(day) || end.hasSameWeekTime(day)
            || start.before(day) && end.after(day)

    operator fun contains(calendar: Calendar): Boolean = contains(WeekDayTime(calendar))

    operator fun contains(time: WeekTime): Boolean = start.beforeOrEqual(time) && end.after(time)

    val isEmpty: Boolean
        get() = start == end

    override fun equals(other: Any?): Boolean {
        if (other is WeekInterval) {
            return other.start == start && other.end == end
        }
        return false
    }

    override fun toString(): String = "[$start - $end]"

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

}
