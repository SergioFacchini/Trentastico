package com.geridea.trentastico.model.cache

/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor
import com.geridea.trentastico.database.*

import com.geridea.trentastico.model.LessonType


class CachedLessonType @JvmOverloads constructor(
        val lesson_type_id: String,
        val name: String,
        val partitioningName: String,
        val color: Int,
        val isAnExtraCourse: Boolean = false) {

    constructor(lessonType: LessonType) : this(lessonType.id, lessonType.name, lessonType.partitioningName, lessonType.color)

    companion object {

        fun fromLessonTypeCursor(cursor: Cursor): CachedLessonType = CachedLessonType(
                cursor.getString(cursor.getColumnIndexOrThrow(CLT_LESSON_TYPE_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(CLT_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(CLT_PARTITIONING_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CLT_COLOR)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CLT_IS_EXTRA_COURSE)) == 0
        )
    }
}
