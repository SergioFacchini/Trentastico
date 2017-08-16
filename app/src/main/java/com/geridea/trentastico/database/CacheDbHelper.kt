package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.geridea.trentastico.Config
import com.geridea.trentastico.utils.AppPreferences

class CacheDbHelper(context: Context) : SQLiteOpenHelper(context, Config.DATABASE_NAME, null, Config.DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_CACHED_PERIOD)
        db.execSQL(SQL_CREATE_CACHED_LESSON_TYPES)
        db.execSQL(SQL_CREATE_CACHED_LESSONS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val cacher = Cacher(db)

        if (oldVersion <= 1) {
            //Fixing bug #57
            cacher.removeExtraCoursesNotInList(AppPreferences.extraCourses)
        }

        if (oldVersion < 3) {
            db.execSQL(SQL_CREATE_CACHED_LIBRARY_TIMES)
        }
    }

}
