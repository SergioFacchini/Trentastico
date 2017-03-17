package com.geridea.trentastico.utils.time;


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import java.util.Calendar;

public class WeekDayTime {
    private int year;
    private int weekNumber;
    private int weekDay;

    public WeekDayTime(int year, int weekNumber, int weekDay) {
        this.year = year;
        this.weekNumber = weekNumber;
        this.weekDay = weekDay;
    }

    public WeekDayTime(Calendar calendar) {
        this(calendar.get(Calendar.YEAR), calendar.get(Calendar.WEEK_OF_YEAR), calendar.get(Calendar.DAY_OF_WEEK));
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public boolean before(WeekDayTime anotherDay) {
        return this.year       < anotherDay.year ||
               this.weekNumber < anotherDay.weekNumber ||
               this.weekDay    < anotherDay.weekDay;
    }
}
