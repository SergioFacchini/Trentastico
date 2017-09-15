package com.geridea.trentastico.utils.time

import java.util.*


/*
 * Created with ♥ by Slava on 15/09/2017.
 */
data class DayOfYear(val dayOfMonth: Int, val month: Int, val year: Int): Comparable<DayOfYear> {

    val millis: Long
      get() {
          val calendar = CalendarUtils.clearCalendar
          calendar.set(Calendar.YEAR, year)
          calendar.set(Calendar.MONTH, month)
          calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

          return calendar.timeInMillis
      }

    override fun compareTo(other: DayOfYear): Int {
        var compare = year.compareTo(other.year)
        if (compare != 0) {
            return compare
        }

        compare = month.compareTo(other.month)
        if (compare != 0) {
            return compare
        }

        compare = dayOfMonth.compareTo(other.dayOfMonth)
        if (compare != 0) {
            return compare
        }

        return 0
    }

    constructor(calendar: Calendar) : this(
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.YEAR)
    )

}