package trentastico.geridea.com.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor;

import trentastico.geridea.com.trentastico.model.LessonSchedule;

public class CachedLesson {

    private long lesson_id;
    private long cached_period_id;
    private long starts_at_ms;
    private long finishes_at_ms;
    private long teaching_id;

    private String subject;
    private String room;
    private String description;

    public CachedLesson(long cached_period_id, long lesson_id, long starts_at_ms, long finishes_at_ms, long teaching_id, String subject, String room, String description) {
        this.lesson_id = lesson_id;
        this.cached_period_id = cached_period_id;
        this.starts_at_ms = starts_at_ms;
        this.finishes_at_ms = finishes_at_ms;
        this.teaching_id = teaching_id;
        this.subject = subject;
        this.room = room;
        this.description = description;
    }

    public CachedLesson(long cachedPeriodId, LessonSchedule lesson) {
        this(
                cachedPeriodId,
                lesson.getId(),
                lesson.getStartsAt(),
                lesson.getFinishesAt(),
                lesson.getLessonTypeId(),
                lesson.getSubject(),
                lesson.getRoom(),
                lesson.getFullDescription()
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

    public static CachedLesson fromCursor(Cursor cursor) {
        return new CachedLesson(
                cursor.getLong(cursor.getColumnIndexOrThrow("cached_period_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("lesson_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("starts_at_ms")),
                cursor.getLong(cursor.getColumnIndexOrThrow("finishes_at_ms")),
                cursor.getLong(cursor.getColumnIndexOrThrow("teaching_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                cursor.getString(cursor.getColumnIndexOrThrow("room")),
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
        );

    }
}
