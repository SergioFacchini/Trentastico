package com.geridea.trentastico.utils.time

import org.junit.Test


/*
 * Created with ♥ by Slava on 20/04/2017.
 */
class WeekTimeTest {

    @Test
    @Throws(Exception::class)
    fun testYearChange() {
        val weekToLoad = WeekTime(2017, 52)
        val loadFrom = weekToLoad.copy()

        val loadTo = loadFrom.copy()
        loadTo.addWeeks(+2)

        WeekInterval(loadFrom, loadTo)
    }

}