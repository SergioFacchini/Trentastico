package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor;

import java.util.Calendar;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;

public class CachedPeriod {

    private long id;
    private long lesson_type = 0;
    private long cached_in_ms;

    private WeekInterval period;

    public CachedPeriod(long id, WeekInterval period, long lesson_type, long cached_in_ms) {
        this.id = id;
        this.period = period;
        this.lesson_type = lesson_type;
        this.cached_in_ms = cached_in_ms;
    }

    public CachedPeriod(WeekInterval interval) {
        this.id = -1;
        this.lesson_type   = 0;
        this.period        = interval.copy();
        this.cached_in_ms  = System.currentTimeMillis();
    }

    public CachedPeriod(long id, int startWeek, int startYear, int endWeek, int endYear, long lesson_type, long cached_in_ms) {
        this.id            = id;
        this.lesson_type   = lesson_type;
        this.cached_in_ms  = cached_in_ms;

        this.period = new WeekInterval(startWeek, startYear, endWeek, endYear);
    }

    public long getId() {
        return id;
    }

    public long getLesson_type() {
        return lesson_type;
    }

    public long getCached_in_ms() {
        return cached_in_ms;
    }

    public void setId(long id) {
        this.id = id;
    }

    public WeekInterval getPeriod() {
        return period;
    }

    public static CachedPeriod fromCursor(Cursor cursor) {
        return new CachedPeriod(
            cursor.getLong(cursor.getColumnIndexOrThrow(Cacher.CP_ID)),
            cursor.getInt( cursor.getColumnIndexOrThrow(Cacher.CP_START_WEEK)),
            cursor.getInt( cursor.getColumnIndexOrThrow(Cacher.CP_START_YEAR)),
            cursor.getInt( cursor.getColumnIndexOrThrow(Cacher.CP_END_WEEK)),
            cursor.getInt( cursor.getColumnIndexOrThrow(Cacher.CP_END_YEAR)),
            cursor.getLong(cursor.getColumnIndexOrThrow(Cacher.CP_LESSON_TYPE)),
            cursor.getLong(cursor.getColumnIndexOrThrow(Cacher.CP_CACHED_IN_MS))
        );
    }

    public void setPeriod(WeekInterval period) {
        this.period = period;
    }

    public CachedPeriod copy() {
        return new CachedPeriod(-1, getPeriod(), lesson_type, cached_in_ms);
    }

    public boolean contains(WeekTime timeToCheck) {
        return period.contains(timeToCheck);
    }

    @Override
    public String toString() {
        return "[id: "+id+" "+period+"]";
    }
}
