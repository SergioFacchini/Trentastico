package com.geridea.trentastico.network.operations;

import com.geridea.trentastico.gui.views.requestloader.AbstractTextMessage;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LessonsLoadingMessage extends AbstractTextMessage {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.ITALIAN);
    private final boolean isARetry;

    private Calendar from, to;

    public LessonsLoadingMessage(int operationId, CalendarInterval interval, boolean isARetry) {
        super(operationId);
        this.from = interval.getFrom();
        this.to   = interval.getTo();
        this.isARetry = isARetry;
    }

    public LessonsLoadingMessage(int operationId, WeekInterval intervalToLoad, boolean isARetry) {
        this(operationId, intervalToLoad.toCalendarInterval(), isARetry);

    }

    @Override
    public String getText() {
        //Usually we're loading data that is exactly one or two weeks longs, considering the day
        //starting at 00:00:00. We don't want to show that day as loaded, so we back by a second
        //to the previous day.
        Calendar adjustedTo = (Calendar) to.clone();
        adjustedTo.add(Calendar.SECOND, -1);

        String message = isARetry ?
                "Sto riprovando a scaricare gli orari dal %s al %s..."
               :"Sto scaricando gli orari dal %s al %s...";

        return String.format(
                message,
                DATE_FORMAT.format(from.getTime()),
                DATE_FORMAT.format(adjustedTo.getTime())
        );
    }
}
