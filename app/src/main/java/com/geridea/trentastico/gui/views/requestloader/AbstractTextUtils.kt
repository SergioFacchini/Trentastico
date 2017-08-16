package com.geridea.trentastico.gui.views.requestloader

import com.geridea.trentastico.utils.time.WeekInterval
import java.text.SimpleDateFormat
import java.util.*


/*
 * Created with ♥ by Slava on 16/08/2017.
 */

private val DATE_FORMAT = SimpleDateFormat("d MMMM", Locale.ITALIAN)

fun formatFromToString(intervalToLoad: WeekInterval): String {
    val ci = intervalToLoad.toCalendarInterval()

    //Usually we're loading data that is exactly one or two weeks longs, considering the day
    //starting at 00:00:00. We don't want to show that day as loaded, so we back by a second
    //to the previous day.
    val adjustedTo = ci.to.clone() as Calendar
    adjustedTo.add(Calendar.SECOND, -1)

    return String.format(
            "%s al %s",
            DATE_FORMAT.format(ci.from.time),
            DATE_FORMAT.format(adjustedTo.time)
    )
}
