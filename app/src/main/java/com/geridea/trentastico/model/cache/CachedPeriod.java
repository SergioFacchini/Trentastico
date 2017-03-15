package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor;

import java.util.Calendar;

import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.LessonsRequest;

public class CachedPeriod {

    private long id;
    private long department_id;
    private long course_id;
    private long year;
    private long from_ms;
    private long to_ms;
    private long lesson_type = 0;
    private long cached_in_ms;

    public CachedPeriod(long id, StudyCourse course, long from_ms, long to_ms, long lesson_type, long cached_in_ms) {
        this.id = id;
        this.department_id = course.getDepartmentId();
        this.course_id     = course.getCourseId();
        this.year          = course.getYear();
        this.from_ms = from_ms;
        this.to_ms = to_ms;
        this.lesson_type = lesson_type;
        this.cached_in_ms = cached_in_ms;
    }

    public CachedPeriod(LessonsRequest request) {
        this.id = -1;

        StudyCourse course = request.getStudyCourse();
        this.department_id = course.getDepartmentId();
        this.course_id     = course.getCourseId();
        this.year          = course.getYear();
        this.lesson_type = 0;

        this.from_ms = request.getFromWhen().getTimeInMillis();
        this.to_ms   = request.getToWhen().getTimeInMillis();

        this.cached_in_ms = System.currentTimeMillis();
    }

    public CachedPeriod(long id, long department_id, long course_id, long year, long from_ms, long to_ms, long lesson_type, long cached_in_ms) {
        this.id            = id;
        this.department_id = department_id;
        this.course_id     = course_id;
        this.year          = year;
        this.from_ms       = from_ms;
        this.to_ms         = to_ms;
        this.lesson_type   = lesson_type;
        this.cached_in_ms  = cached_in_ms;
    }

    public long getId() {
        return id;
    }

    public long getDepartment_id() {
        return department_id;
    }

    public long getCourse_id() {
        return course_id;
    }

    public long getYear() {
        return year;
    }

    public long getFrom_ms() {
        return from_ms;
    }

    public long getTo_ms() {
        return to_ms;
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


    public static CachedPeriod fromCursor(Cursor cursor) {
        return new CachedPeriod(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),

                cursor.getLong(cursor.getColumnIndexOrThrow("department_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("course_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("year")),

                cursor.getLong(cursor.getColumnIndexOrThrow("from_ms")),
                cursor.getLong(cursor.getColumnIndexOrThrow("to_ms")),

                cursor.getLong(cursor.getColumnIndexOrThrow("lesson_type")),
                cursor.getLong(cursor.getColumnIndexOrThrow("cached_in_ms"))
        );
    }

    /**
     * Removes from the current period the part of the period that intersects with the
     * give from-to interval.
     */
    public void cutFromPeriod(long cutFrom, long cutTo) {
        if(cutFrom > from_ms && cutTo < to_ms){
            throw new IllegalStateException("Cannot cut an interval in two pieces");
        } else if(cutFrom == from_ms && cutTo == to_ms){
            from_ms = to_ms; //Exact match: clear everything
        } else if(from_ms <= cutFrom){
            to_ms = cutFrom; //Trimming right side
        } else {
            from_ms = cutTo; //Trimming left side
        }
    }

    public boolean isEmpty() {
        return from_ms - to_ms == 0;
    }

    /**
     * @return true if the given interval is smaller or equal to the period and it's
     * temporarily inside that period.
     */
    public boolean containsEntirely(Calendar fromWeek, Calendar toWeek) {
        return fromWeek.getTimeInMillis() >= from_ms && toWeek.getTimeInMillis() <= to_ms;
    }

}
