package com.geridea.trentastico.utils.time;

import org.junit.Test;


/*
 * Created with ♥ by Slava on 20/04/2017.
 */
public class WeekTimeTest {

    @Test
    public void testYearChange() throws Exception {
        WeekTime weekToLoad = new WeekTime(2017, 52);
        WeekTime loadFrom = weekToLoad.copy();

        WeekTime loadTo = loadFrom.copy();
        loadTo.addWeeks(+2);

        new WeekInterval(loadFrom, loadTo);
    }

}