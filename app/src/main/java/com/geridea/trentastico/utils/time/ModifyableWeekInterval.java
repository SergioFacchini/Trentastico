package com.geridea.trentastico.utils.time;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import java.util.Calendar;

public class ModifyableWeekInterval extends WeekInterval {
    public ModifyableWeekInterval(WeekTime start, WeekTime end) {
        super(start, end);
    }

    public ModifyableWeekInterval(Calendar fromWhen, Calendar toWhen) {
        super(fromWhen, toWhen);
    }

    public ModifyableWeekInterval(int startWeek, int startYear, int endWeek, int endYear) {
        super(startWeek, startYear, endWeek, endYear);
    }

    public ModifyableWeekInterval(int fromDelta, int toDelta) {
        super(fromDelta, toDelta);
    }


}
