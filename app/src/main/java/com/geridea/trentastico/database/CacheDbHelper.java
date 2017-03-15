package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.geridea.trentastico.Config;

public class CacheDbHelper extends SQLiteOpenHelper {

//cached_periods:      id, department_id, course_id, year, from_ms, to_ms, lesson_type, cached_in_ms
//cached_lessons:      lesson_id, room, subject, starts_at_ms, finishes_at_ms, description, teaching_id, cached_period_id
//cached_lesson_types: lesson_type_id, name, color

    private static final String SQL_CREATE_CACHED_PERIOD =
        "CREATE TABLE cached_periods (" +
            "id             INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "department_id  INTEGER NOT NULL, " +
            "course_id      INTEGER NOT NULL, " +
            "year           INTEGER NOT NULL, " +
            "from_ms        INTEGER NOT NULL, " +
            "to_ms          INTEGER NOT NULL, " +
            "lesson_type    INTEGER, " +
            "cached_in_ms   INTEGER NOT NULL" +
        ")";

    private static final String SQL_CREATE_CACHED_LESSONS =
        "CREATE TABLE cached_lessons (" +
            "cached_period_id INTEGER NOT NULL," +
            "lesson_id        INTEGER NOT NULL, " +
            "starts_at_ms     INTEGER NOT NULL, " +
            "finishes_at_ms   INTEGER NOT NULL, " +
            "teaching_id      INTEGER NOT NULL, " +
            "subject          VARCHAR(500) NOT NULL, " +
            "room             VARCHAR(500) NOT NULL, " +
            "description      VARCHAR(500) NOT NULL " +
        ")";

    private static final String SQL_CREATE_CACHED_LESSON_TYPES =
        "CREATE TABLE cached_lesson_types (" +
            "lesson_type_id  INTEGER NOT NULL, " +
            "name            VARCHAR(500) NOT NULL, " +
            "color           INTEGER NOT NULL" +
        ")";

    public CacheDbHelper(Context context) {
        super(context, Config.DATABASE_NAME, null, Config.DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_CACHED_PERIOD);
        db.execSQL(SQL_CREATE_CACHED_LESSON_TYPES);
        db.execSQL(SQL_CREATE_CACHED_LESSONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
