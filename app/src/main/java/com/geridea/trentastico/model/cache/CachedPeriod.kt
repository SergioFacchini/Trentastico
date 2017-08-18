package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.database.Cursor
import com.geridea.trentastico.database.*
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.time.WeekInterval
import com.geridea.trentastico.utils.time.WeekTime

class CachedPeriod {

    var id: Long = 0
    var lesson_type = LESSON_TYPE_NONE.toLong()
        private set
    var cached_in_ms: Long = 0
        private set

    var interval: WeekInterval
        private set

    constructor(id: Long, period: WeekInterval, lesson_type: Long, cached_in_ms: Long) {
        this.id = id
        this.interval = period
        this.lesson_type = lesson_type
        this.cached_in_ms = cached_in_ms
    }

    constructor(interval: WeekInterval) {
        this.id = -1
        this.lesson_type = LESSON_TYPE_NONE.toLong()
        this.interval = interval.copy()
        this.cached_in_ms = System.currentTimeMillis()
    }

    constructor(interval: WeekInterval, lesson_type: Int) {
        this.id = -1
        this.lesson_type = lesson_type.toLong()
        this.interval = interval.copy()
        this.cached_in_ms = System.currentTimeMillis()
    }

    constructor(
            id: Long,
            startWeek: Int,
            startYear: Int,
            endWeek: Int,
            endYear: Int,
            lesson_type: Long,
            cached_in_ms: Long) {
        this.id = id
        this.lesson_type = lesson_type
        this.cached_in_ms = cached_in_ms

        this.interval = WeekInterval(startWeek, startYear, endWeek, endYear)
    }

    fun setPeriod(period: WeekInterval) {
        this.interval = period
    }

    fun copy(): CachedPeriod = CachedPeriod(-1, interval, lesson_type, cached_in_ms)

    operator fun contains(timeToCheck: WeekTime): Boolean = interval.contains(timeToCheck)

    override fun toString(): String = "[id: $id $interval type:$lesson_type]"


    val isStudyCoursePeriod: Boolean
        get() = lesson_type == LESSON_TYPE_NONE.toLong()

    val isExtraCoursePeriod: Boolean
        get() = lesson_type != LESSON_TYPE_NONE.toLong()

    fun canContainStudyLesson(lesson: LessonSchedule): Boolean = lesson_type == LESSON_TYPE_NONE.toLong() && interval!!.contains(lesson.startCal)

    fun canContainExtraLesson(lesson: LessonSchedule, extraCourse: ExtraCourse): Boolean = lesson.lessonTypeId == extraCourse.lessonTypeId.toLong() && interval!!.contains(lesson.startCal)

    companion object {

        private val LESSON_TYPE_NONE = 0

        fun fromCursor(cursor: Cursor): CachedPeriod = CachedPeriod(
                cursor.getLong(cursor.getColumnIndexOrThrow(CP_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CP_START_WEEK)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CP_START_YEAR)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CP_END_WEEK)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CP_END_YEAR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(CP_LESSON_TYPE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(CP_CACHED_IN_MS))
        )
    }
}
