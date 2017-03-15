package com.geridea.trentastico.network.operations;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarLoadingOperation implements ILoadingOperation {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.ITALIAN);

    private Calendar from, to;

    public CalendarLoadingOperation(Calendar from, Calendar to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String describe() {
        //Usually we're loading data that is exactly one or two weeks longs, considering the day
        //starting at 00:00:00. We don't want to show that day as loaded, so we back by a second
        //to the previous day.
        Calendar adjustedTo = (Calendar) to.clone();
        adjustedTo.add(Calendar.SECOND, -1);

        return String.format(
                "Sto scaricando gli orari dal %s al %s...",
                DATE_FORMAT.format(from.getTime()),
                DATE_FORMAT.format(adjustedTo.getTime())
        );
    }
}
