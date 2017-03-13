package trentastico.geridea.com.trentastico.utils;

import java.util.Calendar;

import static trentastico.geridea.com.trentastico.utils.CalendarUtils.formatDDMMYY;

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

    public boolean isInInterval(Calendar time) {
        return from.equals(time) || to.equals(time) || (time.after(from) && time.before(to));
    }

    public boolean matches(Calendar searchedFrom, Calendar searchedTo) {
        return from.equals(searchedFrom) && to.equals(searchedTo);
    }

    @Override
    public String toString() {
        return String.format("[%s-%s]", formatDDMMYY(from), formatDDMMYY(to));
    }

    public Calendar getFrom() {
        return from;
    }

    public Calendar getTo() {
        return to;
    }
}
