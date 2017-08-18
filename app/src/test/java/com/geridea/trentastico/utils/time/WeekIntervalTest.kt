package com.geridea.trentastico.utils.time

import junit.framework.Assert.*
import org.junit.Test
import java.util.*

@Suppress("JoinDeclarationAndAssignment")
/*
 * Created with ♥ by Slava on 17/03/2017.
 */
class WeekIntervalTest {

    @Test
    @Throws(Exception::class)
    fun cut() {
        var cut: WeekIntervalCutResult

        //-----|cache-cache|-------------------
        //--------------------|cut-cut-cut|----
        cut = cutFromInterval(10, 12, 18, 22)
        assertFirstIs(cut, 10, 12)
        assertSecondNull(cut)

        //--------------------|cache-cache|----
        //-----|cut-cut-cut|-------------------
        cut = cutFromInterval(18, 22, 10, 12)
        assertFirstIs(cut, 18, 22)
        assertSecondNull(cut)


        //-------------|cut-cut-cut|----------------
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(10, 20, 12, 18)
        assertFirstIs(cut, 10, 12)
        assertSecondIs(cut, 18, 20)

        //--|cut-cut-cut-cut-cut-cut-cut-cut-cut|---
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(10, 20, 8, 22)
        assertFalse(cut.hasAnyRemainingResult())


        //-|cut-cut-cut|----------------------------
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(7, 15, 5, 10)
        assertFirstIs(cut, 10, 15)
        assertSecondNull(cut)

        cut = cutFromInterval(5, 15, 5, 10)
        assertFirstIs(cut, 10, 15)
        assertSecondNull(cut)

        //--------------------------|cut-cut-cut|---
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(5, 20, 7, 25)
        assertFirstIs(cut, 5, 7)
        assertSecondNull(cut)

        cut = cutFromInterval(5, 20, 7, 20)
        assertFirstIs(cut, 5, 7)
        assertSecondNull(cut)

        //A possible check to verify if we missed some solutions
        val random = Random()
        for (i in 0..9999) {
            val startFrom = random.nextInt(30) + 1
            val endFrom = startFrom + random.nextInt(30) + 1

            val startTo = random.nextInt(30) + 1
            val endTo = startTo + random.nextInt(30) + 1

            cutFromInterval(startFrom, endFrom, startTo, endTo)
        }
    }

    private fun assertSecondNull(cut: WeekIntervalCutResult) = assertNull(cut.secondRemaining)

    private fun assertSecondIs(cut: WeekIntervalCutResult, startWeek: Int, endWeek: Int) {
        val second = cut.secondRemaining
        assertEquals(second, mkInt(startWeek, endWeek))
    }

    private fun assertFirstIs(cut: WeekIntervalCutResult, startWeek: Int, endWeek: Int) {
        val first = cut.firstRemaining
        assertEquals(first, mkInt(startWeek, endWeek))
    }

    private fun cutFromInterval(startFrom: Int, endFrom: Int, startCut: Int, endCut: Int): WeekIntervalCutResult = mkInt(startFrom, endFrom).cutFromInterval(mkInt(startCut, endCut))

    private fun mkInt(startWeek: Int, endWeek: Int): WeekInterval = WeekInterval(startWeek, 2017, endWeek, 2017)

}