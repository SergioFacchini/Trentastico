package com.geridea.trentastico.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.birbit.android.jobqueue.config.Configuration
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.LibraryOpeningTimes
import com.geridea.trentastico.model.Room
import com.geridea.trentastico.network.controllers.listener.CachedLibraryOpeningTimesListener
import com.geridea.trentastico.utils.*
import com.geridea.trentastico.utils.time.CalendarUtils
import org.json.JSONArray
import java.util.*


/*
 * Created with ♥ by Slava on 21/08/2017.
 */

class Cacher(context: Context) {

    private val writableDatabase: SQLiteDatabase = CacheDbHelper(context).writableDatabase

    private val jobQueue: JobManager = JobManager(
            Configuration.Builder(context)
                    .minConsumerCount(1)
                    .maxConsumerCount(1)
                    .build()
    )

    private fun runJobInBackground(jobToRun: () -> Unit){
        jobQueue.addJobInBackground(object: CacheJob() {
            override fun onRun() {
                jobToRun()
            }
        })
    }

    private fun runJobInForeground(jobToRun: () -> Unit){
        jobQueue.addJob(object: CacheJob() {
            override fun onRun() {
                jobToRun()
            }
        })
    }

    //-------------------
    // Lesson schedules
    //-------------------
    fun syncGetStandardLessonsAndTypes(callback: (lessons: List<LessonSchedule>, lessonTypes: List<LessonType>) -> Unit) {
        runJobInForeground {
            callback(_fetchStandardScheduledLesson(), fetchStandardLessonTypes())
        }
    }

    fun getStandardLessonsAndTypes(callback: (lessons: List<LessonSchedule>, lessonTypes: List<LessonType>) -> Unit) {
        runJobInBackground {
            callback(_fetchStandardScheduledLesson(), fetchStandardLessonTypes())
        }
    }

    private fun _fetchStandardScheduledLesson(): MutableList<LessonSchedule> {
        val cursor = writableDatabase.query(SL_TABLE_NAME, scheduledLessonsColumns, "${SL_is_extra} = 0")

        return fetchScheduledLessonsFromCursor(cursor)
    }

    fun cacheExtraScheduledLessons(lessons: Collection<LessonSchedule>) {
        runJobInBackground {
            if (lessons.isNotEmpty()) {
                try {
                    //starting transaction
                    writableDatabase.beginTransaction()

                    //caching lessons
                    purgeExtraScheduledLessons(lessons.first().lessonTypeId)
                    lessons.forEach { cacheScheduledLesson(it, true) }

                    //setting transaction successful
                    writableDatabase.setTransactionSuccessful()
                } finally {
                    writableDatabase.endTransaction()
                }
            }
        }
    }

    private fun cacheScheduledLesson(lesson: LessonSchedule, isExtra: Boolean) {
        val values = ContentValues()
        values.put(SL_id,               lesson.id)
        values.put(SL_room,             lesson.rooms.toJsonStringArray { it.toJson() }.toString())
        values.put(SL_teachersNames,    lesson.teachersNames)
        values.put(SL_subject,          lesson.subject)
        values.put(SL_partitioningName, lesson.partitioningName)
        values.put(SL_startsAt,         lesson.startsAt)
        values.put(SL_endsAt,           lesson.endsAt)
        values.put(SL_lessonTypeId,     lesson.lessonTypeId)

        values.put(SL_is_extra,         isExtra)

        writableDatabase.insert(SL_TABLE_NAME, null, values)
    }

    private fun purgeStandardScheduledLessons() {
        writableDatabase.delete(SL_TABLE_NAME, SL_is_extra +"= 0", arrayOf())
    }

    private fun purgeExtraScheduledLessons(lessonTypeId: String) {
        writableDatabase.delete(SL_TABLE_NAME, SL_lessonTypeId +"= ? AND is_extra = 1", arrayOf(lessonTypeId))
    }

    fun fetchExtraScheduledLessons(lessonTypeId: String, callback: (lessons: List<LessonSchedule>) -> Unit) {
        runJobInBackground {
            callback(_fetchExtraScheduledLessons(lessonTypeId))
        }
    }

    private fun _fetchExtraScheduledLessons(lessonTypeId: String): List<LessonSchedule> {
        val cursor = writableDatabase.query(SL_TABLE_NAME, scheduledLessonsColumns,
                "${SL_is_extra} = 1 AND ${SL_lessonTypeId} = ? ", arrayOf(lessonTypeId)
        )

        return fetchScheduledLessonsFromCursor(cursor)
    }

    private fun fetchScheduledLessonsFromCursor(cursor: Cursor): MutableList<LessonSchedule> {
        val lessons = mutableListOf<LessonSchedule>()
        while (cursor.moveToNext()) {
            lessons.add(LessonSchedule(
                    id               = cursor.getString(SL_id),
                    rooms            = JSONArray(cursor.getString(SL_room)).mapObjects { Room(it) },
                    teachersNames    = cursor.getString(SL_teachersNames),
                    subject          = cursor.getString(SL_subject),
                    partitioningName = cursor.getNullableString(SL_partitioningName),
                    startsAt         = cursor.getLong(SL_startsAt),
                    endsAt           = cursor.getLong(SL_endsAt),
                    lessonTypeId     = cursor.getString(SL_lessonTypeId)
            ))
        }

        cursor.close()
        return lessons
    }

    fun syncLoadTodaysLessons(): List<LessonSchedule> {
        var lessons = mutableListOf<LessonSchedule>()

        runJobInForeground {
            //Fetching standard and extra courses
            lessons = _fetchStandardScheduledLesson()
            lessons.addAll(
                AppPreferences.extraCourses
                    .map { _fetchExtraScheduledLessons(it.lessonTypeId) }
                    .flatten()
            )

            //Removing lessons that are not held today
            val (start, end) = CalendarUtils.getTodaysStartAndEndMs()
            lessons.removeAll { it.startsBefore(start) || it.startsAfter(end) }
            lessons.sortBy { it.startsAt }
        }

        return lessons
    }

    fun loadTodaysLessons(listener: TodaysLessonsListener) {
        runJobInBackground {
            //Fetching standard and extra courses
            val lessons = _fetchStandardScheduledLesson()
            lessons.addAll(
                    AppPreferences.extraCourses
                            .map { _fetchExtraScheduledLessons(it.lessonTypeId) }
                            .flatten()
            )

            //Removing lessons that are not held today
            val (start, end) = CalendarUtils.getTodaysStartAndEndMs()
            lessons.removeAll { it.startsBefore(start) || it.startsAfter(end) }
            lessons.sortBy { it.startsAt }

            listener.onLessonsAvailable(lessons)
        }
    }

    //-------------------
    // Lesson types
    //-------------------

    /**
     * Fetches standard lesson types, if any
     */
    private fun fetchStandardLessonTypes(): List<LessonType> {
        val cursor = writableDatabase.query(LT_TABLE_NAME, lessonTypesColumns)

        val lessonTypes = mutableListOf<LessonType>()
        while (cursor.moveToNext()) {
            val lessonTypeId = cursor.getString(LT_id)
            lessonTypes.add(LessonType(
                    id               = lessonTypeId,
                    name             = cursor.getString(LT_name),
                    teachers         = JSONArray(cursor.getString(LT_teachers)).toStringArray(),
                    kindOfLesson     = cursor.getString(LT_kindOfLesson),
                    partitioningName = cursor.getNullableString(LT_partitioningName),
                    isVisible        = !AppPreferences.isLessonTypeToHide(lessonTypeId)
            ))
        }

        cursor.close()
        return lessonTypes
    }

    fun cacheStandardLessonTypesAndLessons(types: Collection<LessonType>, lessons: List<LessonSchedule>){
        runJobInBackground {
            //Starting transaction
            try {
                writableDatabase.beginTransaction()

                //Caching lesson types
                purgeStandardLessonTypes()
                types.forEach { cacheLessonType(it) }

                //Caching lessons
                purgeStandardScheduledLessons()
                lessons.forEach { cacheScheduledLesson(it, false) }

                //Finishing transaction
                writableDatabase.setTransactionSuccessful()
            } finally {
                //Finishing transaction
                writableDatabase.endTransaction()
            }
        }
    }

    fun fetchLessonTypes(callback: (List<LessonType>) -> Unit) {
        runJobInBackground {
            callback(fetchStandardLessonTypes())
        }
    }


    private fun purgeStandardLessonTypes() {
        writableDatabase.delete(LT_TABLE_NAME, null, null)
    }

    private fun cacheLessonType(lessonType: LessonType) {
            val values = ContentValues()
            values.put(LT_id,               lessonType.id)
            values.put(LT_name,             lessonType.name)
            values.put(LT_teachers,         lessonType.teachers.toJsonStringArray().toString())
            values.put(LT_kindOfLesson,     lessonType.kindOfLesson)
            values.put(LT_partitioningName, lessonType.partitioningName)

            writableDatabase.insert(LT_TABLE_NAME, null, values)
    }

    //-------------------
    // General - Lessons and types
    //-------------------
    /**
     * Deletes everything we have in cache about the current study course (lessons and types)
     */
    fun purgeStudyCourseCache() {
        runJobInBackground {
            purgeStandardLessonTypes()
            purgeStandardScheduledLessons()
        }
    }

    fun removeExtraCoursesWithLessonType(lessonTypeId: String) {
        runJobInBackground {
            purgeExtraScheduledLessons(lessonTypeId)
        }
    }

    fun obliterateCache() {
        runJobInBackground {
            purgeStandardLessonTypes()
            purgeStandardScheduledLessons()

            AppPreferences.extraCourses.forEach {
                purgeExtraScheduledLessons(it.lessonTypeId)
            }
        }
    }


    //-------------------
    // Library opening times
    //-------------------

    /**
     * @return the unix timestamp before which the cache of the opening times of the library is
     * considered to be old.
     */
    private fun calculateLastValidLibraryCachePeriod(): Long {
        val lastValid = CalendarUtils.debuggableToday
        lastValid.add(Calendar.DAY_OF_WEEK, -5)

        return lastValid.timeInMillis
    }

    fun getCachedLibraryOpeningTimes(day: Calendar, fetchDeadCacheToo: Boolean, listener: CachedLibraryOpeningTimesListener){
        //Querying
        val selection = "${CLIBT_DAY} = ? AND ${CLIBT_CACHED_IN_MS} >= ?"

        val lastValidMs = (if (fetchDeadCacheToo) -1 else calculateLastValidLibraryCachePeriod()).toString()
        val selectionArgs = arrayOf(LibraryOpeningTimes.formatDay(day), lastValidMs)


        val cursor = writableDatabase.query(
                CACHED_LIBRARY_TIMES_TABLE_NAME, libraryTimesColumns, selection, selectionArgs
        )
        if (cursor.moveToFirst()) {
            listener.onCachedOpeningTimesFound(buildLibraryOpeningTimesFromCursor(cursor))
        } else {
            listener.onNoCachedOpeningTimes()
        }

        cursor.close()
    }

    private fun buildLibraryOpeningTimesFromCursor(cursor: Cursor): LibraryOpeningTimes {
        val times = LibraryOpeningTimes()
        times.day             = cursor.getString(cursor.getColumnIndex(CLIBT_DAY))
        times.timesBuc        = cursor.getString(cursor.getColumnIndex(CLIBT_BUC))
        times.timesCial       = cursor.getString(cursor.getColumnIndex(CLIBT_CIAL))
        times.timesMesiano    = cursor.getString(cursor.getColumnIndex(CLIBT_MESIANO))
        times.timesPovo       = cursor.getString(cursor.getColumnIndex(CLIBT_POVO))
        times.timesPsicologia = cursor.getString(cursor.getColumnIndex(CLIBT_PSICOLOGIA))
        return times
    }

    fun cacheLibraryOpeningTimes(times: LibraryOpeningTimes){
        runJobInBackground {
            val values = ContentValues()
            values.put(CLIBT_DAY,          times.day)
            values.put(CLIBT_BUC,          times.timesBuc)
            values.put(CLIBT_CIAL,         times.timesCial)
            values.put(CLIBT_MESIANO,      times.timesMesiano)
            values.put(CLIBT_POVO,         times.timesPovo)
            values.put(CLIBT_PSICOLOGIA,   times.timesPsicologia)
            values.put(CLIBT_CACHED_IN_MS, CalendarUtils.debuggableMillis)

            writableDatabase.replace(CACHED_LIBRARY_TIMES_TABLE_NAME, null, values)
        }
    }



}


//Cache Jobs
abstract class CacheJob: Job(Params(1)) {

    override fun onAdded() = Unit

    override fun onCancel(cancelReason: Int, throwable: Throwable?) = Unit

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        throwable.printStackTrace()
        BugLogger.logBug("Throwable raised when running a cache job", Exception(throwable))

        return RetryConstraint.CANCEL
    }

}


//SQL TABLES

// ------------------
// Scheduled lessons
val SL_TABLE_NAME = "scheduled_lessons"

val SL_id               = "id"
val SL_room             = "room"
val SL_teachersNames    = "teachersNames"
val SL_subject          = "subject"
val SL_partitioningName = "partitioningName"
val SL_startsAt         = "startsAt"
val SL_endsAt           = "endsAt"
val SL_lessonTypeId     = "lessonTypeId"
val SL_is_extra         = "is_extra"

val scheduledLessonsColumns = arrayOf(
        SL_id, SL_room, SL_teachersNames, SL_subject, SL_partitioningName,
        SL_startsAt, SL_endsAt, SL_lessonTypeId, SL_is_extra
)

internal val SQL_CREATE_SCHEDULED_LESSONS =
  """CREATE TABLE ${SL_TABLE_NAME} (
      ${SL_id}               VARCHAR(100) PRIMARY KEY,
      ${SL_room}             VARCHAR(500) NOT NULL,
      ${SL_teachersNames}    VARCHAR(200) NOT NULL,
      ${SL_subject}          VARCHAR(100) NOT NULL,
      ${SL_partitioningName} VARCHAR(100),
      ${SL_startsAt}         INT NOT NULL,
      ${SL_endsAt}           INT NOT NULL,
      ${SL_lessonTypeId}     VARCHAR(100) NOT NULL,
      ${SL_is_extra}         INT NOT NULL
  )"""


// ------------------
// Lesson types
val LT_TABLE_NAME = "lesson_types"

val LT_id               = "id"
val LT_name             = "name"
val LT_teachers         = "teachers"
val LT_kindOfLesson     = "kindOfLesson"
val LT_partitioningName = "partitioningName"

val lessonTypesColumns = arrayOf(
        LT_id, LT_name, LT_teachers, LT_teachers, LT_kindOfLesson, LT_partitioningName
)

internal val SQL_CREATE_LESSON_TYPES =
  """CREATE TABLE ${LT_TABLE_NAME} (
      ${LT_id}               VARCHAR(100) PRIMARY KEY,
      ${LT_name}             VARCHAR(150) NOT NULL,
      ${LT_teachers}         VARCHAR(500) NOT NULL,
      ${LT_kindOfLesson}     VARCHAR(50)  NOT NULL,
      ${LT_partitioningName} VARCHAR(100)
  )"""



//Library opening times
val CACHED_LIBRARY_TIMES_TABLE_NAME = "cached_library_times"

val CLIBT_DAY          = "day"
val CLIBT_BUC          = "buc"
val CLIBT_CIAL         = "cial"
val CLIBT_MESIANO      = "mesiano"
val CLIBT_POVO         = "povo"
val CLIBT_PSICOLOGIA   = "psicologia"
val CLIBT_CACHED_IN_MS = "cached_in_ms"

val libraryTimesColumns = arrayOf(
        CLIBT_DAY, CLIBT_BUC, CLIBT_CIAL, CLIBT_MESIANO, CLIBT_POVO, CLIBT_PSICOLOGIA, CLIBT_CACHED_IN_MS
)

internal val SQL_CREATE_CACHED_LIBRARY_TIMES =
        """CREATE TABLE ${CACHED_LIBRARY_TIMES_TABLE_NAME} (
        ${CLIBT_DAY}          CHAR(10) NOT NULL PRIMARY KEY,
        ${CLIBT_BUC}          VARCHAR(10) NOT NULL,
        ${CLIBT_CIAL}         VARCHAR(10) NOT NULL,
        ${CLIBT_MESIANO}      VARCHAR(10) NOT NULL,
        ${CLIBT_POVO}         VARCHAR(10) NOT NULL,
        ${CLIBT_PSICOLOGIA}   VARCHAR(10) NOT NULL,
        ${CLIBT_CACHED_IN_MS} INT NOT NULL )"""
