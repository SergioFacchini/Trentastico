package com.geridea.trentastico.database_new

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.birbit.android.jobqueue.config.Configuration
import com.geridea.trentastico.database.CacheDbHelper
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.utils.*


/*
 * Created with ♥ by Slava on 21/08/2017.
 */

/**
 * The class that deals with the caching of lessons.<br></br>
 * There are 3 types o caches: <br></br>
 *
 *  * **Fresh cache: ** the cache that has just been fetched and didn't expire
 * yet.
 *  * **Old cache: ** the cache that has expired but still can be used if unable
 * to get the lesson from the network.
 *  * **Dead cache: ** the cache that we saved relatively to past period.
 * Technically this kind of cache will never get updated, and it will lie in our cache database
 * indefinitely.
 *
 */
class CacherNew(context: Context) {

    /* What should be cached:
        + Current scheduled lessons
        * Current lesson types
        + Current extra courses scheduled lessons
        * Current extra courses lesson types
     */
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

    //-------------------
    // Lesson schedules
    //-------------------
    fun getStandardLessonsAndTypes(callback: (lessons: List<LessonSchedule>, lessonTypes: List<LessonTypeNew>) -> Unit) {
        runJobInBackground {
            callback(fetchStandardScheduledLesson(), fetchStandardLessonTypes())
        }
    }

    private fun fetchStandardScheduledLesson(): List<LessonSchedule> {
        val cursor = writableDatabase.query(SL_TABLE_NAME, scheduledLessonsColumns, "$SL_is_extra = 0")

        return fetchScheduledLessonsFromCursor(cursor)
    }

    fun cacheStandardScheduledLessons(lessons: Collection<LessonSchedule>) {
        runJobInBackground {
            purgeStandardScheduledLessons()

            lessons.forEach { cacheScheduledLesson(it, false) }
        }
    }

    fun cacheExtraScheduledLessons(lessons: Collection<LessonSchedule>) {
        runJobInBackground {
            if (lessons.isNotEmpty()) {
                purgeExtraScheduledLessons(lessons.first().lessonTypeId)

                lessons.forEach { cacheScheduledLesson(it, true) }
            }
        }

    }

    private fun cacheScheduledLesson(lesson: LessonSchedule, isExtra: Boolean) {
        val values = ContentValues()
        values.put(SL_id,               lesson.id)
        values.put(SL_room,             lesson.room)
        values.put(SL_teachersNames,    lesson.teachersNames)
        values.put(SL_subject,          lesson.subject)
        values.put(SL_partitioningName, lesson.partitioningName)
        values.put(SL_startsAt,         lesson.startsAt)
        values.put(SL_endsAt,           lesson.endsAt)
        values.put(SL_color,            lesson.color)
        values.put(SL_lessonTypeId,     lesson.lessonTypeId)

        values.put(SL_is_extra,         isExtra)

        writableDatabase.insert(SL_TABLE_NAME, null, values)
    }

    private fun purgeStandardScheduledLessons() {
        writableDatabase.delete(SL_TABLE_NAME, SL_is_extra+"= 0", arrayOf())
    }

    private fun purgeExtraScheduledLessons(lessonTypeId: String) {
        writableDatabase.delete(SL_TABLE_NAME, SL_lessonTypeId+"= ?", arrayOf(lessonTypeId))
    }

    fun fetchExtraScheduledLessons(lessonTypeId: String, callback: (lessons: List<LessonSchedule>) -> Unit) {
        runJobInBackground {
            val cursor = writableDatabase.query(SL_TABLE_NAME, scheduledLessonsColumns,
                    "$SL_is_extra = 1 AND $SL_lessonTypeId = ? ", arrayOf(lessonTypeId)
            )

            callback(fetchScheduledLessonsFromCursor(cursor))
        }
    }

    private fun fetchScheduledLessonsFromCursor(cursor: Cursor): List<LessonSchedule> {
        val lessons = mutableListOf<LessonSchedule>()
        while (cursor.moveToNext()) {
            lessons.add(LessonSchedule(
                    id               = cursor.getString(SL_id),
                    room             = cursor.getString(SL_room),
                    teachersNames    = cursor.getString(SL_teachersNames),
                    subject          = cursor.getString(SL_subject),
                    partitioningName = cursor.getNullableString(SL_partitioningName),
                    startsAt         = cursor.getLong(SL_startsAt),
                    endsAt           = cursor.getLong(SL_endsAt),
                    color            = cursor.getInt(SL_color),
                    lessonTypeId     = cursor.getString(SL_lessonTypeId)
            ))
        }

        cursor.close()
        return lessons
    }

    //-------------------
    // Lesson types
    //-------------------

    /**
     * Fetches standard lesson types, if any
     */
    private fun fetchStandardLessonTypes(): List<LessonTypeNew> {
        val cursor = writableDatabase.query(LT_TABLE_NAME, lessonTypesColumns)

        val lessonTypes = mutableListOf<LessonTypeNew>()
        while (cursor.moveToNext()) {
            val lessonTypeId = cursor.getString(LT_id)
            lessonTypes.add(LessonTypeNew(
                    id               = lessonTypeId,
                    name             = cursor.getString(LT_name),
                    teachersNames    = cursor.getString(LT_teachersNames),
                    partitioningName = cursor.getNullableString(LT_partitioningName),
                    color            = cursor.getInt(LT_color),
                    isVisible        = !AppPreferences.isLessonTypeToHide(lessonTypeId)
            ))
        }

        cursor.close()
        return lessonTypes
    }

    fun cacheStandardLessonTypes(lessonTypeNew: Collection<LessonTypeNew>){
        runJobInBackground {
            purgeStandardLessonTypes()

            lessonTypeNew.forEach { cacheLessonType(it) }
        }
    }

    private fun purgeStandardLessonTypes() {
        writableDatabase.delete(LT_TABLE_NAME, null, null)
    }

    private fun cacheLessonType(lessonTypeNew: LessonTypeNew) {
            val values = ContentValues()
            values.put(LT_id,               lessonTypeNew.id)
            values.put(LT_name,             lessonTypeNew.name)
            values.put(LT_teachersNames,    lessonTypeNew.teachersNames)
            values.put(LT_partitioningName, lessonTypeNew.partitioningName)
            values.put(LT_color,            lessonTypeNew.color)

            writableDatabase.insert(LT_TABLE_NAME, null, values)
    }

}

//Cache Jobs
abstract class CacheJob: Job(Params(Cacher.PRIORITY_NORMAL)) {

    override fun onAdded() = Unit

    override fun onCancel(cancelReason: Int, throwable: Throwable?) = Unit

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        throwable.printStackTrace()
        BugLogger.logBug("Throwable raised when running a job", throwable)

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
val SL_color            = "color"
val SL_lessonTypeId     = "lessonTypeId"
val SL_is_extra         = "is_extra"

val scheduledLessonsColumns = arrayOf(
        SL_id, SL_room, SL_teachersNames, SL_subject, SL_partitioningName,
        SL_startsAt, SL_endsAt, SL_color, SL_lessonTypeId, SL_is_extra
)

internal val SQL_CREATE_SCHEDULED_LESSONS =
  """CREATE TABLE $SL_TABLE_NAME (
      $SL_id               VARCHAR(100) PRIMARY KEY,
      $SL_room             VARCHAR(150) NOT NULL,
      $SL_teachersNames    VARCHAR(200) NOT NULL,
      $SL_subject          VARCHAR(100) NOT NULL,
      $SL_partitioningName VARCHAR(100),
      $SL_startsAt         INT NOT NULL,
      $SL_endsAt           INT NOT NULL,
      $SL_color            INT NOT NULL,
      $SL_lessonTypeId     VARCHAR(100) NOT NULL,
      $SL_is_extra         INT NOT NULL
  )"""


// ------------------
// Lesson types
val LT_TABLE_NAME = "lesson_types"

val LT_id               = "id"
val LT_name             = "name"
val LT_teachersNames    = "teachersNames"
val LT_partitioningName = "partitioningName"
val LT_color            = "color"

val lessonTypesColumns = arrayOf(
    LT_id, LT_name, LT_teachersNames, LT_partitioningName, LT_color
)

internal val SQL_CREATE_LESSON_TYPES =
  """CREATE TABLE $LT_TABLE_NAME (
      $LT_id               VARCHAR(100) PRIMARY KEY,
      $LT_name             VARCHAR(150) NOT NULL,
      $LT_teachersNames    VARCHAR(200) NOT NULL,
      $LT_partitioningName VARCHAR(100),
      $LT_color            INT NOT NULL
  )"""


