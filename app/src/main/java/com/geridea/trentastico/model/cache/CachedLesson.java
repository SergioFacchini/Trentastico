package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.utils.time.WeekTime;

public class CachedLesson {

    private long lesson_id;
    private long cached_period_id;
    private long starts_at_ms;
    private long finishes_at_ms;
    private long teaching_id;
    private String subject;
    private int color;

    private String room;
    private String description;

    private final WeekTime weekTime;

    public CachedLesson(long cached_period_id, long lesson_id, long starts_at_ms, long finishes_at_ms, long teaching_id, String subject, String room, String description, int color) {
        this.lesson_id = lesson_id;
        this.cached_period_id = cached_period_id;
        this.starts_at_ms = starts_at_ms;
        this.finishes_at_ms = finishes_at_ms;
        this.teaching_id = teaching_id;
        this.subject = subject;
        this.room = room;
        this.description = description;
        this.color = color;

        this.weekTime = new WeekTime(starts_at_ms);
    }

    public CachedLesson(long cachedPeriodId, LessonSchedule lesson) {
        this(
                cachedPeriodId,
                lesson.getId(),
                lesson.getStartsAt(),
                lesson.getEndsAt(),
                lesson.getLessonTypeId(),
                lesson.getSubject(),
                lesson.getRoom(),
                lesson.getFullDescription(),
                lesson.getColor()
        );
    }

    public long getLesson_id() {
        return lesson_id;
    }

    public long getCached_period_id() {
        return cached_period_id;
    }

    public long getStarts_at_ms() {
        return starts_at_ms;
    }

    public long getFinishes_at_ms() {
        return finishes_at_ms;
    }

    public long getTeaching_id() {
        return teaching_id;
    }

    public String getSubject() {
        return subject;
    }

    public String getRoom() {
        return room;
    }

    public String getDescription() {
        return description;
    }

    public WeekTime getWeekTime() {
        return weekTime;
    }

    public static CachedLesson fromCursor(Cursor cursor) {
        return new CachedLesson(
            cursor.getLong(  cursor.getColumnIndexOrThrow(Cacher.CL_CACHED_PERIOD_ID)),
            cursor.getLong(  cursor.getColumnIndexOrThrow(Cacher.CL_LESSON_ID)),
            cursor.getLong(  cursor.getColumnIndexOrThrow(Cacher.CL_STARTS_AT_MS)),
            cursor.getLong(  cursor.getColumnIndexOrThrow(Cacher.CL_FINISHES_AT_MS)),
            cursor.getLong(  cursor.getColumnIndexOrThrow(Cacher.CL_TEACHING_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(Cacher.CL_SUBJECT)),
            cursor.getString(cursor.getColumnIndexOrThrow(Cacher.CL_ROOM)),
            cursor.getString(cursor.getColumnIndexOrThrow(Cacher.CL_DESCRIPTION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(Cacher.CL_COLOR))
        );

    }

    public int getColor() {
        return color;
    }
}
