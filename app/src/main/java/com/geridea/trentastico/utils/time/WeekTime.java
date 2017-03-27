package com.geridea.trentastico.utils.time;


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import java.util.Calendar;

public class WeekTime {
    private int year;
    private int weekNumber;

    public WeekTime(int year, int weekNumber) {
        this.year = year;
        this.weekNumber = weekNumber;
    }

    public WeekTime() {
        initFromCalendar(Calendar.getInstance());
    }

    public WeekTime(Calendar calendar) {
        initFromCalendar(calendar);
    }

    public WeekTime(long milliseconds) {
        Calendar dummy = CalendarUtils.getClearDummyCalendar();
        dummy.setTimeInMillis(milliseconds);

        initFromCalendar(dummy);
    }

    public WeekTime(WeekDayTime weekDayTime) {
        this(weekDayTime.getYear(), weekDayTime.getWeekNumber());
    }

    public void initFromCalendar(Calendar calendar) {
        year       = calendar.get(Calendar.YEAR);
        weekNumber = calendar.get(Calendar.WEEK_OF_YEAR);
    }

    public int getYear() {
        return year;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    /**
     * @return true if the current week time start after the argument.
     */
    public boolean after(WeekTime arg) {
        if (this.year == arg.year) {
            return this.weekNumber > arg.weekNumber;
        } else
            return this.year > arg.year;
    }

    /**
     * @return true if the current week time start before the argument.
     */
    public boolean before(WeekTime arg) {
        if (this.year == arg.year) {
            return this.weekNumber < arg.weekNumber;
        } else
            return this.year < arg.year;
    }


    /**
     * Creates a new <code>WeekTime</code> that is referring to the current week.
     */
    public static WeekTime getCurrentInstance() {
        return new WeekTime(CalendarUtils.getCachedToday());
    }

    public boolean hasSameWeekTime(WeekDayTime day) {
        return getWeekNumber() == day.getWeekNumber() &&
                     getYear() == day.getYear();
    }

    public boolean before(WeekDayTime day) {
        if (this.year == day.getYear()) {
            return this.weekNumber < day.getWeekNumber();
        } else
            return this.year < day.getYear();
    }

    public boolean after(WeekDayTime day) {
        if (this.year == day.getYear()) {
            return this.weekNumber > day.getWeekNumber();
        } else
            return this.year > day.getYear();
    }


    public void addWeeks(int numWeeksToAdd) {
        //Some years may have 53 weeks, so it's a good thing to delegate this calculation to the
        //calendar
        Calendar dummy = CalendarUtils.getClearDummyCalendar();
        dummy.set(Calendar.YEAR, year);
        dummy.set(Calendar.WEEK_OF_YEAR, weekNumber);
        dummy.add(Calendar.WEEK_OF_YEAR, numWeeksToAdd);

        initFromCalendar(dummy);
    }

    public WeekTime copy() {
        return new WeekTime(year, weekNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WeekTime) {
            WeekTime time = (WeekTime) obj;
            return time.weekNumber == weekNumber && time.year == year;
        }
        return false;
    }

    /**
     * True if the current week time start before or equals the argument.
     */
    public boolean beforeOrEqual(WeekTime weekTime) {
        return before(weekTime) || equals(weekTime);
    }

    /**
     * True if the current week time start after or equals the argument.
     */
    public boolean afterOrEqual(WeekTime weekTime) {
        return after(weekTime) || equals(weekTime);
    }

    @Override
    public String toString() {
        return year+"/"+weekNumber;
    }
}
