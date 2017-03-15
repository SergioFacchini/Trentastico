
package com.geridea.trentastico.model.cache;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor;

import com.geridea.trentastico.model.LessonType;

public class CachedLessonType {

    private int lesson_type_id;
    private String name;
    private int color;

    public CachedLessonType(int lesson_type_id, String name, int color) {
        this.lesson_type_id = lesson_type_id;
        this.name = name;
        this.color = color;
    }

    public CachedLessonType(LessonType lessonType) {
        this(lessonType.getId(),
             lessonType.getName(),
             lessonType.getColor());
    }

    public int getLesson_type_id() {
        return lesson_type_id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public static CachedLessonType fromCursor(Cursor cursor) {
        return new CachedLessonType(
            cursor.getInt(cursor.getColumnIndexOrThrow("lesson_type_id")),
            cursor.getString(cursor.getColumnIndexOrThrow("name")),
            cursor.getInt(cursor.getColumnIndexOrThrow("color"))
        );
    }
}
