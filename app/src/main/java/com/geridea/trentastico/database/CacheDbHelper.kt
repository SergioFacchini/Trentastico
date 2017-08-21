package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.geridea.trentastico.Config
import com.geridea.trentastico.database_new.SQL_CREATE_LESSON_TYPES
import com.geridea.trentastico.database_new.SQL_CREATE_SCHEDULED_LESSONS

class CacheDbHelper(context: Context) : SQLiteOpenHelper(context, Config.DATABASE_NAME, null, Config.DATABASE_VERSION) {


    override fun onCreate(db: SQLiteDatabase) {
        //Calendar
        db.execSQL(SQL_CREATE_SCHEDULED_LESSONS)
        db.execSQL(SQL_CREATE_LESSON_TYPES)

        //Library times
        db.execSQL(SQL_CREATE_CACHED_LIBRARY_TIMES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) { ; }

}
