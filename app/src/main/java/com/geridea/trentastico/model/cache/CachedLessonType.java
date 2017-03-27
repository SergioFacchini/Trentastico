
package com.geridea.trentastico.model.cache;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor;

import com.geridea.trentastico.model.LessonType;

import static com.geridea.trentastico.database.Cacher.CLT_COLOR;
import static com.geridea.trentastico.database.Cacher.CLT_IS_EXTRA_COURSE;
import static com.geridea.trentastico.database.Cacher.CLT_LESSON_TYPE_ID;
import static com.geridea.trentastico.database.Cacher.CLT_NAME;

public class CachedLessonType {

    private int lesson_type_id;
    private String name;
    private int color;

    private boolean isAnExtraCourse = false;

    public CachedLessonType(int lesson_type_id, String name, int color) {
        this(lesson_type_id, name, color, false);
    }

    public CachedLessonType(int lesson_type_id, String name, int color, boolean isAnExtraCourse) {
        this.lesson_type_id = lesson_type_id;
        this.name = name;
        this.color = color;
        this.isAnExtraCourse = isAnExtraCourse;
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

    public boolean isAnExtraCourse() {
        return isAnExtraCourse;
    }

    public static CachedLessonType fromLessonTypeCursor(Cursor cursor) {
        return new CachedLessonType(
            cursor.getInt(   cursor.getColumnIndexOrThrow(CLT_LESSON_TYPE_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(CLT_NAME)),
            cursor.getInt(   cursor.getColumnIndexOrThrow(CLT_COLOR)),
            (cursor.getInt(   cursor.getColumnIndexOrThrow(CLT_IS_EXTRA_COURSE)) == 0)
        );
    }
}
