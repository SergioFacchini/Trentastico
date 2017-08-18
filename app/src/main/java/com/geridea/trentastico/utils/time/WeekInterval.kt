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

    constructor(fromWhen: Calendar, toWhen: Calendar) : this(WeekTime(fromWhen), WeekTime(toWhen))

    constructor(
            startWeek: Int,
            startYear: Int,
            endWeek: Int,
            endYear: Int) : this(WeekTime(startYear, startWeek), WeekTime(endYear, endWeek))

    constructor(fromDelta: Int, toDelta: Int) {
        val from = WeekTime()
        from.addWeeks(fromDelta)

        val to = WeekTime()
        to.addWeeks(toDelta + 1)

        init(from, to)
    }

    val startCopy: WeekTime
        get() = start.copy()

    val endCopy: WeekTime
        get() = end.copy()

    fun toCalendarInterval(): CalendarInterval {
        val from = CalendarUtils.purgedCalendarInstance
        from.set(Calendar.YEAR, start.year)
        from.set(Calendar.WEEK_OF_YEAR, start.weekNumber)

        val to = CalendarUtils.purgedCalendarInstance
        to.set(Calendar.YEAR, end.year)
        to.set(Calendar.WEEK_OF_YEAR, end.weekNumber)

        return CalendarInterval(from, to)
    }

    operator fun contains(day: WeekDayTime): Boolean = start.hasSameWeekTime(day) || end.hasSameWeekTime(day)
            || start.before(day) && end.after(day)

    operator fun contains(calendar: Calendar): Boolean = contains(WeekDayTime(calendar))

    operator fun contains(time: WeekTime): Boolean = start.beforeOrEqual(time) && end.after(time)

    fun copy(): WeekInterval = WeekInterval(start.copy(), end.copy())

    val isEmpty: Boolean
        get() = start == end

    /**
     * @param toCut the interval to cut from the period
     * @return the intervals containing the intervals that survived. May return
     * an empty period (in case the operation had cut everything), leave the period intact (in case
     * the period to cut was outside this period), return two periods (in case the period
     * we're trying to cut divides in half this interval) or return the period remaining from
     * the cut.
     */
    fun cutFromInterval(toCut: WeekInterval): WeekIntervalCutResult {
        val cutResult: WeekIntervalCutResult

        val intStart = start
        val intEnd = end

        val cutStart = toCut.startCopy
        val cutEnd = toCut.endCopy

        //-----|cache-cache|-------------------
        //--------------------|cut-cut-cut|----
        // OR
        //-----|cut-cut-cut|-------------------
        //--------------------|cache-cache|----
        if (cutEnd.beforeOrEqual(intStart) || cutStart.afterOrEqual(intEnd)) {
            cutResult = WeekIntervalCutResult(
                    WeekInterval.empty(),
                    this.copy()
            )

            //-------------|cut-cut-cut|----------------
            //-----|cache-cache-cache-cache-cache|------
        } else if (cutStart.after(intStart) && cutEnd.before(intEnd)) {
            cutResult = WeekIntervalCutResult(
                    toCut,
                    WeekInterval(intStart, cutStart),
                    WeekInterval(cutEnd, intEnd)
            )

            //--|cut-cut-cut-cut-cut-cut-cut-cut-cut|---
            //-----|cache-cache-cache-cache-cache|------
        } else if (cutStart.beforeOrEqual(intStart) && cutEnd.afterOrEqual(intEnd)) {
            //We're cutting more or exactly our interval: nothing to keep!
            cutResult = WeekIntervalCutResult(this)

            //-|cut-cut-cut|----------------------------
            //-----|cache-cache-cache-cache-cache|------
        } else if (cutStart.beforeOrEqual(intStart) && contains(cutEnd)) {
            cutResult = WeekIntervalCutResult(
                    WeekInterval(intStart, cutEnd),
                    WeekInterval(cutEnd, intEnd)
            )

            //--------------------------|cut-cut-cut|---
            //-----|cache-cache-cache-cache-cache|------
        } else if (contains(cutStart) && cutEnd.afterOrEqual(intEnd)) {
            cutResult = WeekIntervalCutResult(
                    WeekInterval(cutStart, intEnd),
                    WeekInterval(intStart, cutStart)
            )
        } else {
            //Should never happen!
            throw RuntimeException("Unable to cut the interval!")
        }

        return cutResult
    }

    /**
     * @return an iterator that iterates on all the week times of the interval, starting from the
     * start (included) and finishing with the end (excluded).
     */
    //The last week is not included
    val iterator: Iterator<WeekTime>
        get() {
            val iterator = this.start.copy()
            iterator.addWeeks(-1)

            val end = this.end.copy()
            end.addWeeks(-1)

            return object : Iterator<WeekTime> {
                override fun hasNext(): Boolean = iterator.before(end)

                override fun next(): WeekTime {
                    iterator.addWeeks(+1)
                    return iterator
                }
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other is WeekInterval) {
            return other.start == start && other.end == end
        }
        return false
    }

    override fun toString(): String = "[$start - $end]"


    val startWeekNumber: Int
        get() = start.weekNumber

    val startYear: Int
        get() = start.year

    val endWeekNumber: Int
        get() = end.weekNumber

    val endYear: Int
        get() = end.year

    /**
     * @return true if this interval has at least one week in common with the interval passes as
     * parameter
     */
    fun overlaps(intervalToCheck: WeekInterval): Boolean = equals(intervalToCheck) || cutFromInterval(intervalToCheck).hasAnyRemainingResult()

    fun spansMultipleYears(): Boolean = startYear != endYear

    fun intersect(interval: WeekInterval): WeekInterval = cutFromInterval(interval).cutInterval

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

    companion object {
        private val EMPTY_WEEK = WeekInterval(WeekTime(0, 0), WeekTime(0, 0))

        private fun empty(): WeekInterval = EMPTY_WEEK
    }
}
