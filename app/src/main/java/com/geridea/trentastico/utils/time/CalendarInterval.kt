package com.geridea.trentastico.utils.time

import java.util.Calendar

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
class CalendarInterval {
    val from: Calendar
    val to: Calendar

    constructor(from: Calendar, to: Calendar) {
        this.from = from
        this.to = to
    }

    constructor(from: Long, to: Long) {
        this.from = CalendarUtils.getCalendarInitializedAs(from)
        this.to = CalendarUtils.getCalendarInitializedAs(to)
    }

    operator fun contains(time: Calendar): Boolean {
        return time.after(from) && time.before(to) || from == time
    }

    fun matches(searchedFrom: Calendar, searchedTo: Calendar): Boolean {
        return from == searchedFrom && to == searchedTo
    }

    override fun toString(): String {
        return String.format("[%s-%s]", CalendarUtils.formatDDMMYY(from), CalendarUtils.formatDDMMYY(to))
    }

    val fromMs: Long
        get() = from.timeInMillis

    val toMs: Long
        get() = to.timeInMillis
}
