package com.geridea.trentastico.utils.time

import java.util.*


/*
 * Created with ♥ by Slava on 15/09/2017.
 */
data class DayInterval(val from: DayOfYear, val to: DayOfYear) {

    constructor(from: Calendar, to: Calendar): this(DayOfYear(from), DayOfYear(to))

    operator fun contains(millis: Long): Boolean = millis >= from.millis && millis <= to.millis

}