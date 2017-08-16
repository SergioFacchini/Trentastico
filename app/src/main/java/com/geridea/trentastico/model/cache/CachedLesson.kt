package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor
import com.geridea.trentastico.database.*
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.time.WeekTime

class CachedLesson(
        val cached_period_id: Long,
        val lesson_id: Long,
        val starts_at_ms: Long,
        val finishes_at_ms: Long,
        val teaching_id: Long,
        val subject: String,
        val room: String?,
        val description: String,
        val color: Int) {

    val weekTime: WeekTime = WeekTime(starts_at_ms)

    constructor(cachedPeriodId: Long, lesson: LessonSchedule) : this(
            cachedPeriodId,
            lesson.id,
            lesson.startsAt,
            lesson.endsAt,
            lesson.lessonTypeId,
            lesson.subject,
            lesson.room,
            lesson.fullDescription,
            lesson.color
    )

    companion object {

        fun fromCursor(cursor: Cursor): CachedLesson {
            return CachedLesson(
                    cursor.getLong(cursor.getColumnIndexOrThrow(CL_CACHED_PERIOD_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(CL_LESSON_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(CL_STARTS_AT_MS)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(CL_FINISHES_AT_MS)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(CL_TEACHING_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CL_SUBJECT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CL_ROOM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CL_DESCRIPTION)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CL_COLOR))
            )

        }
    }
}
