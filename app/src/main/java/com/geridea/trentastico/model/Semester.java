package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import java.util.Calendar;

public final class Semester {

    private static final int SEMESTER1_START = 9;
    private static final int SEMESTER1_END   = 1;

    private static final int SEMESTER2_START = 2;
    private static final int SEMESTER2_END   = 8;

    private static final Calendar TODAY = Calendar.getInstance();
    private static final Semester CURRENT_SEMESTER = new Semester(TODAY);

    private int year;
    private int semesterNumber;

    public Semester (Calendar date) {
        this(getSemesterNumber(date.get(Calendar.MONTH)), date.get(Calendar.YEAR));
    }

    public Semester(int year, int semesterNumber) {
        this.year = year;
        this.semesterNumber = semesterNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Semester){
            Semester anotherSemester = (Semester) obj;
            return this.year == anotherSemester.year
                && this.semesterNumber == anotherSemester.semesterNumber;
        }

        return false;
    }

    public static boolean isInCurrentSemester(Calendar date){
        return CURRENT_SEMESTER.enclosesDate(date);
    }

    private boolean enclosesDate(Calendar date) {
        return year == date.get(Calendar.YEAR) && getSemesterNumber(date) == semesterNumber;
    }

    public static int getSemesterNumber(Calendar date) {
        return getSemesterNumber(date.get(Calendar.MONTH));
    }

    public static int getSemesterNumber(int monthNumber) {
        if (monthNumber >= SEMESTER2_START && monthNumber <= SEMESTER2_END){
            return 2;
        } else {
            return 1;
        }
    }

}
