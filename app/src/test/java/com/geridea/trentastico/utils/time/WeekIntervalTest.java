package com.geridea.trentastico.utils.time;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/*
 * Created with ♥ by Slava on 17/03/2017.
 */
public class WeekIntervalTest {

    @Test
    public void cut() throws Exception {
        WeekIntervalCutResult cut;

        //-----|cache-cache|-------------------
        //--------------------|cut-cut-cut|----
        cut = cutFromInterval(10, 12, 18, 22);
        assertFirstIs(cut, 10, 12);
        assertSecondNull(cut);

        //--------------------|cache-cache|----
        //-----|cut-cut-cut|-------------------
        cut = cutFromInterval(18, 22, 10, 12);
        assertFirstIs(cut, 18, 22);
        assertSecondNull(cut);


        //-------------|cut-cut-cut|----------------
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(10, 20, 12, 18);
        assertFirstIs (cut, 10, 12);
        assertSecondIs(cut, 18, 20);

        //--|cut-cut-cut-cut-cut-cut-cut-cut-cut|---
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(10, 20, 8, 22);
        assertFalse(cut.hasAnyRemainingResult());


        //-|cut-cut-cut|----------------------------
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(7, 15, 5, 10);
        assertFirstIs(cut, 10, 15);
        assertSecondNull(cut);

        cut = cutFromInterval(5, 15, 5, 10);
        assertFirstIs(cut, 10, 15);
        assertSecondNull(cut);

        //--------------------------|cut-cut-cut|---
        //-----|cache-cache-cache-cache-cache|------
        cut = cutFromInterval(5, 20, 7, 25);
        assertFirstIs(cut, 5, 7);
        assertSecondNull(cut);

        cut = cutFromInterval(5, 20, 7, 20);
        assertFirstIs(cut, 5, 7);
        assertSecondNull(cut);

        //A possible check to verify if we missed some solutions
        Random random = new Random();
        for(int i = 0; i < 10000; i++){
            int startFrom = random.nextInt(30) + 1;
            int endFrom   = startFrom + random.nextInt(30) + 1;

            int startTo = random.nextInt(30) + 1;
            int endTo   = startTo + random.nextInt(30) + 1;

            cutFromInterval(startFrom, endFrom, startTo, endTo);
        }
    }

    private void assertSecondNull(WeekIntervalCutResult cut) {
        assertNull(cut.getSecondRemaining());
    }

    private void assertSecondIs(WeekIntervalCutResult cut, int startWeek, int endWeek) {
        WeekInterval second = cut.getSecondRemaining();
        assertEquals(second, mkInt(startWeek, endWeek));
    }

    private void assertFirstIs(WeekIntervalCutResult cut, int startWeek, int endWeek) {
        WeekInterval first  = cut.getFirstRemaining();
        assertEquals(first, mkInt(startWeek, endWeek));
    }

    private WeekIntervalCutResult cutFromInterval(int startFrom, int endFrom, int startCut, int endCut) {
        return mkInt(startFrom, endFrom).cutFromInterval(mkInt(startCut, endCut));
    }

    @NonNull
    private WeekInterval mkInt(int startWeek, int endWeek) {
        return new WeekInterval(startWeek, 2017, endWeek, 2017);
    }

}