package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 28/04/2017.
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LibraryOpeningTimes {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The day this opening times are referring about. In yyyy-MM-dd format.
     */
    public String day;

    public String timesBuc;
    public String timesCial;
    public String timesMesiano;
    public String timesPovo;
    public String timesPsicologia;

    /**
     * Formats the day in such a way toe make it suitable for the URL request and the storing in
     * cache database.
     * @param day the day to format
     * @return the formatted day, in yyyy-MM-dd format
     */
    public static String formatDay(Calendar day) {
        return dateFormat.format(day.getTime());
    }
}
