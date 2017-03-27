package com.geridea.trentastico.network.operations;

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ExtraCoursesLoadingOperation implements ILoadingOperation {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.ITALIAN);

    private Calendar from;
    private Calendar to;

    private final ExtraCourse course;

    public ExtraCoursesLoadingOperation(CalendarInterval interval, ExtraCourse course) {
        this.from = interval.getFrom();
        this.to   = interval.getTo();

        this.course = course;
    }

    public ExtraCoursesLoadingOperation(WeekInterval intervalToLoad, ExtraCourse extraCourse) {
        this(intervalToLoad.toCalendarInterval(), extraCourse);
    }

    @Override
    public String describe() {
        //Usually we're loading data that is exactly one or two weeks longs, considering the day
        //starting at 00:00:00. We don't want to show that day as loaded, so we back by a second
        //to the previous day.
        Calendar adjustedTo = (Calendar) to.clone();
        adjustedTo.add(Calendar.SECOND, -1);

        return String.format(
                "Sto scaricando gli orari del corso \"%s\" dal %s al %s...",
                course.getName(),
                DATE_FORMAT.format(from.getTime()),
                DATE_FORMAT.format(adjustedTo.getTime())
        );
    }
}
