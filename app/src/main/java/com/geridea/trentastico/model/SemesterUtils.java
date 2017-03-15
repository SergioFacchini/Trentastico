package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import java.util.Calendar;

public final class SemesterUtils {

    private static final int SEMESTER1_START = 9;
    private static final int SEMESTER1_END   = 1;

    private static final int SEMESTER2_START = 2;
    private static final int SEMESTER2_END   = 8;

    private static final Calendar TODAY = Calendar.getInstance();

    public static boolean isInCurrentSemester(Calendar date){
        return getCurrentSemester() == getSemester(date);
    }

    public static int getSemester(Calendar date) {
        return getSemester(date.get(Calendar.MONTH));
    }

    public static int getCurrentSemester() {
        return getSemester(TODAY.get(Calendar.MONTH));

    }

    public static int getSemester(int monthNumber) {
        if (monthNumber >= SEMESTER2_START && monthNumber <= SEMESTER2_END){
            return 2;
        } else {
            return 1;
        }
    }

}
