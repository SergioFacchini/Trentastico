package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.utils.AppPreferences;

public class CacheDbHelper extends SQLiteOpenHelper {

    public CacheDbHelper(Context context) {
        super(context, Config.DATABASE_NAME, null, Config.DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Cacher.SQL_CREATE_CACHED_PERIOD);
        db.execSQL(Cacher.SQL_CREATE_CACHED_LESSON_TYPES);
        db.execSQL(Cacher.SQL_CREATE_CACHED_LESSONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cacher.setTemporaryWritableDatabase(db);

        if (oldVersion <= 1) {
            //Fixing bug #57
            Cacher.removeExtraCoursesNotInList(AppPreferences.getExtraCourses());
        }
    }

}
