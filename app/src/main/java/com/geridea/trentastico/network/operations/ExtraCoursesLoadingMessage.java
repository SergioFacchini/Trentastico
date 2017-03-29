package com.geridea.trentastico.network.operations;

import com.geridea.trentastico.gui.views.requestloader.AbstractTextMessage;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.network.request.ExtraLessonsRequest;
import com.geridea.trentastico.utils.time.CalendarInterval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ExtraCoursesLoadingMessage extends AbstractTextMessage {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.ITALIAN);

    private Calendar from;
    private Calendar to;

    private final ExtraCourse course;
    private final boolean isARetry;

    public ExtraCoursesLoadingMessage(ExtraLessonsRequest request) {
        super(request.getOperationId());

        CalendarInterval interval = request.getIntervalToLoad().toCalendarInterval();
        this.from = interval.getFrom();
        this.to   = interval.getTo();

        this.course = request.getExtraCourse();
        this.isARetry = request.isRetrying();
    }


    @Override
    public String getText() {
        //Usually we're loading data that is exactly one or two weeks longs, considering the day
        //starting at 00:00:00. We don't want to show that day as loaded, so we back by a second
        //to the previous day.
        Calendar adjustedTo = (Calendar) to.clone();
        adjustedTo.add(Calendar.SECOND, -1);

        String format = isARetry ?
                 "Sto riprovando a scaricare gli orari del corso \"%s\" dal %s al %s..."
               : "Sto scaricando gli orari del corso \"%s\" dal %s al %s...";


        return String.format(
                format,
                course.getName(),
                DATE_FORMAT.format(from.getTime()),
                DATE_FORMAT.format(adjustedTo.getTime())
        );
    }

}
