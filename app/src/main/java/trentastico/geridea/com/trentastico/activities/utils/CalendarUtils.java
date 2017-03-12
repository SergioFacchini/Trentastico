package trentastico.geridea.com.trentastico.activities.utils;

import android.support.annotation.NonNull;

import java.util.Calendar;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public class CalendarUtils {

    /**
     * @return the first day of the current week, at 00:00:00.
     */
    @NonNull
    public static Calendar calculateFirstDayOfWeek() {
        return calculateFirstDayOfWeek(Calendar.getInstance());
    }

    /**
     * @return the first day of the week containing the specified date, at 00:00:00.
     */
    @NonNull
    public static Calendar calculateFirstDayOfWeek(Calendar date) {
        Calendar firstDayOfWeek = (Calendar) date.clone();
        firstDayOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        firstDayOfWeek.clear(Calendar.MINUTE);
        firstDayOfWeek.clear(Calendar.SECOND);
        firstDayOfWeek.clear(Calendar.MILLISECOND);
        firstDayOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return firstDayOfWeek;
    }
}
