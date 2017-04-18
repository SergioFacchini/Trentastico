package com.geridea.trentastico.utils.time;


/*
 * Created with ♥ by Slava on 15/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger;

import java.util.Calendar;
import java.util.Iterator;

public class WeekInterval {
    private static WeekInterval EMPTY_WEEK = new WeekInterval(new WeekTime(0, 0), new WeekTime(0, 0));

    private WeekTime start, end;

    public WeekInterval(WeekTime start, WeekTime end) {
        init(start, end);
    }

    private void init(WeekTime start, WeekTime end) {
        if(start.after(end)){
            IllegalStateException e = new IllegalStateException("Cannot have an interval starting after it's end!");
            BugLogger.logBug("Got an interval starting before it's end: "+start+"---"+end, e);
            throw e;
        }

        this.start = start;
        this.end = end;
    }

    public WeekInterval(Calendar fromWhen, Calendar toWhen) {
        this(new WeekTime(fromWhen), new WeekTime(toWhen));
    }

    public WeekInterval(int startWeek, int startYear, int endWeek, int endYear) {
        this(new WeekTime(startYear, startWeek), new WeekTime(endYear, endWeek));
    }

    public WeekInterval(int fromDelta, int toDelta) {
        WeekTime from = new WeekTime();
        from.addWeeks(fromDelta);

        WeekTime to = new WeekTime();
        to.addWeeks(toDelta+1);

        init(from, to);
    }

    public WeekTime getStartCopy() {
        return start.copy();
    }

    public WeekTime getEndCopy() {
        return end.copy();
    }

    public CalendarInterval toCalendarInterval() {
        Calendar from = CalendarUtils.getPurgedCalendarInstance();
        from.set(Calendar.YEAR,         start.getYear());
        from.set(Calendar.WEEK_OF_YEAR, start.getWeekNumber());

        Calendar to = CalendarUtils.getPurgedCalendarInstance();
        to.set(Calendar.YEAR,         end.getYear());
        to.set(Calendar.WEEK_OF_YEAR, end.getWeekNumber());

        return new CalendarInterval(from, to);
    }

    public boolean contains(WeekDayTime day) {
        return start.hasSameWeekTime(day) || end.hasSameWeekTime(day)
            || (start.before(day) && end.after(day));
    }

    public boolean contains(Calendar calendar) {
        return contains(new WeekDayTime(calendar));
    }

    public boolean contains(WeekTime time) {
        return start.beforeOrEqual(time) && end.after(time);
    }

    public WeekInterval copy() {
        return new WeekInterval(start.copy(), end.copy());
    }

    public boolean isEmpty() {
        return start.equals(end);
    }

    /**
     * @param toCut the interval to cut from the period
     * @return the intervals containing the intervals that survived. May return
     * an empty period (in case the operation had cut everything), leave the period intact (in case
     * the period to cut was outside this period), return two periods (in case the period
     * we're trying to cut divides in half this interval) or return the period remaining from
     * the cut.
     */
    public WeekIntervalCutResult cutFromInterval(WeekInterval toCut) {
        WeekIntervalCutResult cutResult;

        WeekTime intStart = start;
        WeekTime intEnd   = end;

        WeekTime cutStart = toCut.getStartCopy();
        WeekTime cutEnd   = toCut.getEndCopy();

        //-----|cache-cache|-------------------
        //--------------------|cut-cut-cut|----
        // OR
        //-----|cut-cut-cut|-------------------
        //--------------------|cache-cache|----
        if(cutEnd.beforeOrEqual(intStart) || cutStart.afterOrEqual(intEnd)){
            cutResult = new WeekIntervalCutResult(
                    WeekInterval.empty(),
                    this.copy()
            );

            //-------------|cut-cut-cut|----------------
            //-----|cache-cache-cache-cache-cache|------
        } else if(cutStart.after(intStart) && cutEnd.before(intEnd)){
            cutResult = new WeekIntervalCutResult(
                    toCut,
                    new WeekInterval(intStart, cutStart),
                    new WeekInterval(cutEnd, intEnd)
            );

            //--|cut-cut-cut-cut-cut-cut-cut-cut-cut|---
            //-----|cache-cache-cache-cache-cache|------
        } else if(cutStart.beforeOrEqual(intStart) && cutEnd.afterOrEqual(intEnd)){
            //We're cutting more or exactly our interval: nothing to keep!
            cutResult = new WeekIntervalCutResult(this);

            //-|cut-cut-cut|----------------------------
            //-----|cache-cache-cache-cache-cache|------
        } else if(cutStart.beforeOrEqual(intStart) && contains(cutEnd)){
            cutResult = new WeekIntervalCutResult(
                    new WeekInterval(intStart, cutEnd),
                    new WeekInterval(cutEnd, intEnd)
            );

            //--------------------------|cut-cut-cut|---
            //-----|cache-cache-cache-cache-cache|------
        } else if(contains(cutStart) && cutEnd.afterOrEqual(intEnd)){
            cutResult = new WeekIntervalCutResult(
                    new WeekInterval(cutStart, intEnd),
                    new WeekInterval(intStart, cutStart)
            );
        } else {
            //Should never happen!
            throw new RuntimeException("Unable to cut the interval!");
        }

        return cutResult;
    }

    private static WeekInterval empty() {
        return EMPTY_WEEK;
    }

    /**
     * @return an iterator that iterates on all the week times of the interval, starting from the
     * start (included) and finishing with the end (excluded).
     */
    public Iterator<WeekTime> getIterator() {
        final WeekTime iterator = this.start.copy();
        iterator.addWeeks(-1);

        final WeekTime end = this.end.copy();
        end.addWeeks(-1); //The last week is not included

        return new Iterator<WeekTime>() {
            @Override
            public boolean hasNext() {
                return iterator.before(end);
            }

            @Override
            public WeekTime next() {
                iterator.addWeeks(+1);
                return iterator;
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof WeekInterval){
            WeekInterval wi = (WeekInterval) obj;
            return wi.start.equals(start) && wi.end.equals(end);
        }
        return false;
    }

    @Override
    public String toString() {
        return "["+start+" - "+end+"]";
    }


    public int getStartWeekNumber() {
        return start.getWeekNumber();
    }

    public int getStartYear() {
        return start.getYear();
    }

    public int getEndWeekNumber() {
        return end.getWeekNumber();
    }

    public int getEndYear() {
        return end.getYear();
    }

    /**
     * @return true if this interval has at least one week in common with the interval passes as
     * parameter
     */
    public boolean overlaps(WeekInterval intervalToCheck) {
        return equals(intervalToCheck) || cutFromInterval(intervalToCheck).hasAnyRemainingResult();
    }

    public boolean spansMultipleYears() {
        return getStartYear() != getEndYear();
    }

    public WeekInterval intersect(WeekInterval interval) {
        return cutFromInterval(interval).getCutInterval();
    }
}
