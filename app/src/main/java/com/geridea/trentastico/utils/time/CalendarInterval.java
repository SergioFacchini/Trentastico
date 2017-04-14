package com.geridea.trentastico.utils.time;

import java.util.Calendar;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public class CalendarInterval {
    private final Calendar from;
    private final Calendar to;

    public CalendarInterval(Calendar from, Calendar to) {
        this.from = from;
        this.to = to;
    }

    public CalendarInterval(long from, long to) {
        this.from = CalendarUtils.getCalendarInitializedAs(from);
        this.to   = CalendarUtils.getCalendarInitializedAs(to);
    }

    public boolean contains(Calendar time) {
        return (time.after(from) && time.before(to)) || from.equals(time);
    }

    public boolean matches(Calendar searchedFrom, Calendar searchedTo) {
        return from.equals(searchedFrom) && to.equals(searchedTo);
    }

    @Override
    public String toString() {
        return String.format("[%s-%s]", CalendarUtils.formatDDMMYY(from), CalendarUtils.formatDDMMYY(to));
    }

    public Calendar getFrom() {
        return from;
    }

    public Calendar getTo() {
        return to;
    }

    public long getFromMs() {
        return getFrom().getTimeInMillis();
    }

    public long getToMs(){
        return getTo().getTimeInMillis();
    }
}
