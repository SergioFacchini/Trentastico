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

    public boolean contains(Calendar time) {
        return from.equals(time) || (time.after(from) && time.before(to));
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
}
