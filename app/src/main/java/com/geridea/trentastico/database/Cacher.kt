package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.birbit.android.jobqueue.config.Configuration
import com.geridea.trentastico.Config
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.*
import com.geridea.trentastico.model.cache.*
import com.geridea.trentastico.network.controllers.listener.CachedLibraryOpeningTimesListener
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.StringUtils
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.*
import java.util.*

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
class Cacher {

    private var writableDatabase: SQLiteDatabase
    private lateinit var jobQueue: JobManager

    constructor(context: Context) {
        writableDatabase = CacheDbHelper(context).writableDatabase

        jobQueue = JobManager(
                Configuration.Builder(context)
                        .minConsumerCount(1)
                        .maxConsumerCount(1)
                        .build()
        )
    }

    /**
     * Use this constructor only during database updates. We aware that this constructor does not
     * generates the job queue for parallel executions. Using the methods that use the JobQueue will
     * result in a null pointer exception.
     * @param db the database to assign
     */
    constructor(db: SQLiteDatabase) {
        writableDatabase = db
    }


    fun cacheExtraLessonsSet(
            setToCache: LessonsSet,
            interval: WeekInterval,
            extraCourse: ExtraCourse) =
            jobQueue.addJobInBackground(CacheExtraLessonsSetJob(setToCache, interval, extraCourse))

    fun cacheLessonsSet(
            setToCache: LessonsSet,
            intervalToCache: WeekInterval) =
            jobQueue.addJobInBackground(CacheLessonsSetJob(setToCache, intervalToCache))

    private fun updateLessonTypesFromSet(
            containsExtraLessonTypes: Boolean,
            lessonTypes: Collection<LessonType>) {

        for (lessonType in lessonTypes) {
            deleteLessonTypeWithId(lessonType.id)
            cacheLessonType(CachedLessonType(lessonType), containsExtraLessonTypes)
        }
    }

    private fun cacheLessonType(
            cachedLessonType: CachedLessonType,
            isExtraCourse: Boolean) {
        val values = ContentValues()
        values.put(CLT_LESSON_TYPE_ID, cachedLessonType.lesson_type_id)
        values.put(CLT_NAME, cachedLessonType.name)
        values.put(CLT_PARTITIONING_NAME, cachedLessonType.partitioningName)
        values.put(CLT_COLOR, cachedLessonType.color)
        values.put(CLT_IS_EXTRA_COURSE, isExtraCourse)

        writableDatabase.insert(CACHED_LESSON_TYPES_TABLE, null, values)
    }

    private fun deleteLessonTypeWithId(lessonTypeId: String) {
        val selection = CLT_LESSON_TYPE_ID + " = ?"
        val selectionArgs = arrayOf(lessonTypeId)

        writableDatabase.delete(CACHED_LESSON_TYPES_TABLE, selection, selectionArgs)
    }

    private fun cacheLesson(cachedLesson: CachedLesson) {
        val values = ContentValues()
        values.put(CL_CACHED_PERIOD_ID, cachedLesson.cached_period_id)
        values.put(CL_LESSON_ID, cachedLesson.lesson_id)
        values.put(CL_STARTS_AT_MS, cachedLesson.starts_at_ms)
        values.put(CL_FINISHES_AT_MS, cachedLesson.finishes_at_ms)
        values.put(CL_WEEK_NUMBER, cachedLesson.weekTime.weekNumber)
        values.put(CL_YEAR, cachedLesson.weekTime.year)
        values.put(CL_TEACHING_ID, cachedLesson.teaching_id)
        values.put(CL_SUBJECT, cachedLesson.subject)
        values.put(CL_ROOM, cachedLesson.room)
        values.put(CL_DESCRIPTION, cachedLesson.description)
        values.put(CL_COLOR, cachedLesson.color)

        writableDatabase.insert(CACHED_LESSONS_TABLE, null, values)
    }

    private fun deleteCachedExtraLessonsInInterval(
            interval: WeekInterval,
            extraCourse: ExtraCourse) {
        //Trimming existing cached periods so the passed interval won't have any overlapping
        //cached interval
        for (period in loadExtraCourseCachePeriods(interval, true, extraCourse.lessonTypeId)) {
            val cutResult = period.interval.cutFromInterval(interval)

            if (!cutResult.hasAnyRemainingResult()) {
                deleteCachedPeriodWithId(period.id)
            } else if (cutResult.hasOnlyOneResult()) {
                period.setPeriod(cutResult.firstRemaining!!)
                updateCachedPeriod(period)
            } else {
                splitCachePeriod(period, cutResult)
            }

            deleteExtraCourseLessonsInInterval(period.id, interval, extraCourse.lessonTypeId)
        }

    }

    private fun deleteCachedStudyCourseLessonsInInterval(interval: WeekInterval) {
        //Trimming existing cached periods so the passed interval won't have any overlapping
        //cached interval
        for (period in loadStudyCourseCachePeriods(interval, true)) {
            val cutResult = period.interval.cutFromInterval(interval)

            if (!cutResult.hasAnyRemainingResult()) {
                deleteCachedPeriodWithId(period.id)
            } else if (cutResult.hasOnlyOneResult()) {
                period.setPeriod(cutResult.firstRemaining!!)
                updateCachedPeriod(period)
            } else {
                splitCachePeriod(period, cutResult)
            }

            deleteStudyCourseLessonsInInterval(period.id, interval)
        }
    }

    /**
     * Splits the cached period in two periods. This means that all the schedules associated to
     * the cache period will be adapted to the new periods; this will generate new instances of
     * cache periods in the database and re-associate the the cached lessons accordingly.<br></br>
     * WARNING: this method does NOT deletes the cached lessons that will be left unassociated due
     * to the split! These will be left in the database associated to the original id. It's the duty
     * of the caller to delete these records.
     */
    private fun splitCachePeriod(
            originalPeriod: CachedPeriod,
            cutResult: WeekIntervalCutResult) {
        originalPeriod.setPeriod(cutResult.firstRemaining!!)
        updateCachedPeriod(originalPeriod)

        val newPeriod = originalPeriod.copy()
        newPeriod.setPeriod(cutResult.secondRemaining!!)
        insertCachedPeriod(newPeriod)

        associateCachedLessonsToAnotherPeriod(originalPeriod.id, newPeriod.id, newPeriod.interval)
    }

    private fun associateCachedLessonsToAnotherPeriod(
            originalPeriodId: Long,
            newPeriodId: Long,
            periodToMove: WeekInterval?) {
        if (periodToMove!!.isEmpty) {
            throw IllegalArgumentException(
                    "Cannot reassociate cached lessons to an empty period!"
            )
        }

        val values = ContentValues()
        values.put(CL_CACHED_PERIOD_ID, newPeriodId)

        val selection = String.format(CL_CACHED_PERIOD_ID + " = ? AND (%s)", buildWeekTimeSelectionsSQL(periodToMove))
        val selectionArgs = arrayOf(originalPeriodId.toString())

        writableDatabase.update(CACHED_LESSONS_TABLE, values, selection, selectionArgs)
    }

    private fun updateCachedPeriod(period: CachedPeriod) {
        val values = getCachedPeriodContentValues(period)
        values.put(CP_ID, period.id)

        // Which row to update, based on the title
        val selection = CP_ID + " = ?"
        val selectionArgs = arrayOf(period.id.toString())

        writableDatabase.update(CACHED_PERIOD_TABLE, values, selection, selectionArgs)
    }

    private fun getCachedPeriodContentValues(period: CachedPeriod): ContentValues {
        val values = ContentValues()
        values.put(CP_START_WEEK,   period.interval.startWeekNumber)
        values.put(CP_START_YEAR,   period.interval.startYear)
        values.put(CP_END_WEEK,     period.interval.endWeekNumber)
        values.put(CP_END_YEAR,     period.interval.endYear)
        values.put(CP_PERIOD_TYPE,  period.cachedPeriodType)
        values.put(CP_CACHED_IN_MS, period.cached_in_ms)
        return values
    }

    private fun deleteCachedPeriodWithId(periodId: Long) {
        val selectionArgs = arrayOf(periodId.toString())
        writableDatabase.delete(CACHED_PERIOD_TABLE, CP_ID + "= ?", selectionArgs)
    }

    private fun deleteExtraCourseLessonsInInterval(
            periodId: Long,
            interval: WeekInterval,
            lessonTypeId: String) =
            deleteLessonsInIntervalInternal(periodId, interval, lessonTypeId)

    private fun deleteLessonsInIntervalInternal(
            periodId: Long,
            interval: WeekInterval?,
            lessonTypeId: String?) {
        var whereClause = String.format("(%s = ?) ", CL_CACHED_PERIOD_ID)

        if (interval != null) {
            whereClause += String.format(" AND (%s) ", buildWeekTimeSelectionsSQL(interval))
        }

        if (lessonTypeId != null) {
            whereClause += "AND $CL_TEACHING_ID = $lessonTypeId"
        }

        val whereArgs = arrayOf(periodId.toString())
        writableDatabase.delete(CACHED_LESSONS_TABLE, whereClause, whereArgs)
    }

    private fun deleteStudyCourseLessonsInInterval(
            periodId: Long,
            interval: WeekInterval) = deleteLessonsInIntervalInternal(periodId, interval, null)

    private fun deleteStudyCourseLessons(periodId: Long) =
            deleteLessonsInIntervalInternal(periodId, null, null)

    private fun deleteExtraLessonsOfType(
            periodId: Long,
            lessonTypeId: String) = deleteLessonsInIntervalInternal(periodId, null, lessonTypeId)

    /**
     * WARNING: this method can load periods that are bigger than the requested one!
     * These periods might contain lessons that are not requested and have to be filtered
     * out.
     */
    private fun loadStudyCourseCachePeriods(
            intervalToLoad: WeekInterval,
            fetchOldCacheToo: Boolean): ArrayList<CachedPeriod> =
            loadCachePeriodsInternal(intervalToLoad, fetchOldCacheToo, null)

    /**
     * WARNING: this method can load periods that are bigger than the requested one!
     * These periods might contain lessons that are not requested and have to be filtered
     * out.
     */
    private fun loadExtraCourseCachePeriods(
            intervalToLoad: WeekInterval,
            fetchOldCacheToo: Boolean,
            lessonTypeId: String): ArrayList<CachedPeriod> =
            loadCachePeriodsInternal(intervalToLoad, fetchOldCacheToo, lessonTypeId)

    /**
     * WARNING: this method can load periods that are bigger than the requested one, but never
     * smaller. These periods might contain lessons that are not requested and have to be filtered
     * out.
     */
    private fun loadCachePeriodsInternal(
            interval: WeekInterval,
            fetchOldCacheToo: Boolean,
            lessonTypeId: String?): ArrayList<CachedPeriod> {
        val projection = arrayOf(CP_ID, CP_START_WEEK, CP_START_YEAR, CP_END_WEEK, CP_END_YEAR, CP_PERIOD_TYPE, CP_CACHED_IN_MS)

        //Building main query:
        var query: String
        if (interval.spansMultipleYears()) {
            //Note: che cached interval might never span more than two different year.
            //Note 2: this method does not considers the cases when the interval that starts in the
            //previous starts before the start of the interval. This is such a rare case, I don't
            //bother to fix this
            query = buildCachePeriodSelectionForInterval(interval)
        } else {
            query = StringUtils.positionFormat(
                    CP_START_YEAR + " = {0} AND " +
                            CP_END_YEAR + " = {0} AND (" +
                            "    (" + CP_START_WEEK + " >= {1} AND " + CP_START_WEEK + "< {2}) OR " +
                            "    (" + CP_END_WEEK + "   >  {1} AND " + CP_END_WEEK + " <= {2}) OR " +
                            "    (" + CP_START_WEEK + " <= {1} AND " + CP_END_WEEK + " >= {2}) OR " +
                            "    (" + CP_START_WEEK + " >= {1} AND " + CP_END_WEEK + " <= {2})    " +
                            ") ", interval.startYear, interval.startWeekNumber, interval.endWeekNumber
            )
        }

        //Adding lesson type filtering
        query += if (lessonTypeId == null) {
            //null = standard study course (not extra)
            " AND {$CP_PERIOD_TYPE} IS NULL "
        } else {
            " AND {$CP_PERIOD_TYPE} = $lessonTypeId "
        }

        //Adding timing filter
        if (!fetchOldCacheToo) {
            query += String.format(" AND (%s >= %d AND %s < %d) ",
                    CP_CACHED_IN_MS, lastValidCacheMs, CP_CACHED_IN_MS, System.currentTimeMillis()
            )
        }

        val cursor = writableDatabase.query(
                CACHED_PERIOD_TABLE, projection, query, null, null, null, null
        )

        val periods = ArrayList<CachedPeriod>()
        while (cursor.moveToNext()) {
            periods.add(CachedPeriod.fromCursor(cursor))
        }
        cursor.close()

        return periods
    }

    private fun buildCachePeriodSelectionForInterval(intervalToLoad: WeekInterval): String {
        val startFrom = intervalToLoad.startCopy
        val startTo = intervalToLoad.endCopy
        val intervalContainingStart = WeekInterval(startFrom, startTo)

        val endFrom = intervalToLoad.startCopy
        endFrom.addWeeks(1)
        val endTo = intervalToLoad.endCopy
        val intervalContainingEnd = WeekInterval(endFrom, endTo)

        val startStr = buildWeekTimeSelectionsSQL(intervalContainingStart, CP_START_WEEK, CP_START_YEAR)
        val endStr = buildWeekTimeSelectionsSQL(intervalContainingEnd, CP_END_WEEK, CP_END_YEAR)

        var expression = startStr
        if (startStr.isEmpty() && endStr.isEmpty()) {
            BugLogger.logBug("\"Start and end interval invalid in query!\"", RuntimeException("Start and end interval invalid in query!"))
            return ""
        } else if (endStr.isEmpty()) {
            //Can happen when fetching a one-week interval
            expression += startStr
        } else if (startStr.isEmpty()) {
            expression += endStr
        } else {
            expression = startStr + " OR " + endStr
        }
        return expression
    }

    private val lastValidCacheMs: Long
        get() = System.currentTimeMillis() - FRESH_CACHE_DURATION_MS

    private fun insertCachedPeriod(cachedPeriod: CachedPeriod): Long {
        val values = getCachedPeriodContentValues(cachedPeriod)

        val id = writableDatabase.insert(CACHED_PERIOD_TABLE, null, values)
        cachedPeriod.id = id

        return id
    }

    fun getLessonsInFreshOrDeadCacheAsync(
            intervalToLoad: WeekInterval,
            extraCourses: ArrayList<ExtraCourse>,
            fetchOldCacheToo: Boolean,
            listener: (CachedLessonsSet) -> Unit) = jobQueue.addJobInBackground(
            GetLessonsInFreshOrDeadCacheJob(intervalToLoad, extraCourses, fetchOldCacheToo, listener)
    )

    private fun getLessonsInFreshOrDeadCache(
            intervalToLoad: WeekInterval,
            extraCourses: ArrayList<ExtraCourse>,
            fetchOldCacheToo: Boolean): CachedLessonsSet {

        val lessonsSet = CachedLessonsSet()

        //Lesson types
        lessonsSet.addCachedLessonTypes(loadLessonTypes(true, false))

        //Study course cached periods
        val studyCourse = AppPreferences.studyCourse
        val studyCoursePeriods = loadStudyCourseCachePeriods(intervalToLoad, fetchOldCacheToo)
        for (cachedPeriod in studyCoursePeriods) {
            val lessons = loadLessonsOfCachePeriod(cachedPeriod, intervalToLoad)
            lessonsSet.addLessonSchedules(lessons)

            //Here we load an intersection of the cached period and the interval we wanted to load,
            //not the whole cached period
            val cachedPeriodIntersection = intervalToLoad.intersect(cachedPeriod.interval)
            lessonsSet.addCachedPeriod(StudyCourseCachedInterval(cachedPeriodIntersection, studyCourse, lessons))
        }

        //Missing study course intervals
        lessonsSet.addMissingIntervals(
                toStudyCourseInterval(findMissingIntervalsInCachePeriods(intervalToLoad, studyCoursePeriods))
        )

        ////////////////
        //Extra courses
        lessonsSet.addCachedLessonTypes(loadExtraCoursesLessonTypes())

        //Calculating missing intervals for all the extra study courses
        for (extraCourse in extraCourses) {
            //Loading cached periods
            val extraPeriods = loadExtraCourseCachePeriods(
                    intervalToLoad, fetchOldCacheToo, extraCourse.lessonTypeId
            )

            //Adding to lessons' set what's cached
            for (cachedPeriod in extraPeriods) {
                val cachedLessons = loadLessonsOfCachePeriod(cachedPeriod, intervalToLoad)
                lessonsSet.addLessonSchedules(cachedLessons)

                val cachedPeriodIntersection = intervalToLoad.intersect(cachedPeriod.interval)
                lessonsSet.addCachedPeriod(ExtraCourseCachedInterval(cachedPeriodIntersection, extraCourse, cachedLessons))
            }

            //Finding not cached extra intervals
            val missingIntervals = findMissingIntervalsInCachePeriods(intervalToLoad, extraPeriods)
            lessonsSet.addMissingIntervals(toExtraCourseInterval(missingIntervals, extraCourse))
        }

        lessonsSet.recalculatePartitionings()

        return lessonsSet
    }

    private fun getLessonsInPeriod(
            from: Long,
            to: Long): ArrayList<CachedLesson> {
        val projection = arrayOf(CL_CACHED_PERIOD_ID, CL_LESSON_ID, CL_STARTS_AT_MS, CL_FINISHES_AT_MS, CL_TEACHING_ID, CL_WEEK_NUMBER, CL_YEAR, CL_SUBJECT, CL_ROOM, CL_DESCRIPTION, CL_COLOR)

        val selection = String.format("%s >= ? AND %s <= ?", CL_STARTS_AT_MS, CL_STARTS_AT_MS)
        val args = arrayOf(from.toString(), to.toString())

        val cursor = writableDatabase.query(
                CACHED_LESSONS_TABLE, projection, selection, args, null, null, CL_STARTS_AT_MS
        )

        val lessons = getCachedLessonsFromCursor(cursor)

        cursor.close()

        return lessons
    }

    private fun loadExtraCoursesLessonTypes(): ArrayList<CachedLessonType> =
            loadLessonTypes(false, true)

    private fun toStudyCourseInterval(intervals: List<WeekInterval>): ArrayList<NotCachedInterval> {
        val notCachedIntervals = ArrayList<NotCachedInterval>()
        for (interval in intervals) {
            notCachedIntervals.add(StudyCourseNotCachedInterval(interval))
        }

        return notCachedIntervals
    }

    private fun toExtraCourseInterval(
            intervals: List<WeekInterval>,
            extraCourse: ExtraCourse): ArrayList<NotCachedInterval> {
        val notCachedIntervals = ArrayList<NotCachedInterval>()
        for (interval in intervals) {
            notCachedIntervals.add(ExtraCourseNotCachedInterval(interval, extraCourse))
        }

        return notCachedIntervals
    }

    private fun loadLessonsOfCachePeriod(
            cachePeriod: CachedPeriod,
            intervalToLoad: WeekInterval): ArrayList<LessonSchedule> =
            cachedLessonsToLessonSchedule(
                    loadCachedLessonsIntersectingInterval(cachePeriod, intervalToLoad)
            )

    private fun cachedLessonsToLessonSchedule(cachedLessons: ArrayList<CachedLesson>): ArrayList<LessonSchedule> {
        val lessons = ArrayList<LessonSchedule>()
        for (cachedLesson in cachedLessons) {
            lessons.add(LessonSchedule(cachedLesson))
        }
        return lessons
    }

    private fun findMissingIntervalsInCachePeriods(
            intervalToLoad: WeekInterval,
            cachePeriods: ArrayList<CachedPeriod>): ArrayList<WeekInterval> {

        val missingIntervals = ArrayList<WeekInterval>()

        var isMissingIntervalBeingBuild = false
        var missingStart: WeekTime? = null
        var missingEnd: WeekTime? = null

        val iterator = intervalToLoad.iterator
        while (iterator.hasNext()) {
            val timeToCheck = iterator.next()

            //is there any cached period that contains the week time?
            if (cachePeriods.any { it.contains(timeToCheck) }) {
                //If we're building a missing interval, let's close it because we found an end
                if (isMissingIntervalBeingBuild) {
                    missingIntervals.add(WeekInterval(missingStart!!, missingEnd!!))
                    isMissingIntervalBeingBuild = false
                }
            } else {
                //We start building an interval of missing weeks
                if (isMissingIntervalBeingBuild) {
                    missingEnd!!.addWeeks(1)
                } else {
                    isMissingIntervalBeingBuild = true
                    missingStart = timeToCheck.copy()

                    missingEnd = timeToCheck.copy()
                    missingEnd.addWeeks(+1)
                }
            }
        }

        //If we were building a missing interval, we must close it
        if (isMissingIntervalBeingBuild) {
            missingIntervals.add(WeekInterval(missingStart!!, missingEnd!!))
        }
        return missingIntervals
    }

    private fun loadLessonTypes(
            loadStudyCourses: Boolean,
            loadExtraCourses: Boolean): ArrayList<CachedLessonType> {
        if (!loadStudyCourses && !loadExtraCourses) {
            throw IllegalArgumentException("Cannot load nothing!")
        }

        val projection = arrayOf(
            CLT_LESSON_TYPE_ID, CLT_NAME, CLT_PARTITIONING_NAME, CLT_COLOR, CLT_IS_EXTRA_COURSE
        )

        val where: String?
        val selectionArgs: Array<String>?

        if (loadStudyCourses && loadExtraCourses) {
            where = null
            selectionArgs = null
        } else if (loadStudyCourses) {
            where = CLT_IS_EXTRA_COURSE + " = ?"
            selectionArgs = arrayOf("0")
        } else {
            //if(loadExtraCourses)
            where = CLT_IS_EXTRA_COURSE + " = ?"
            selectionArgs = arrayOf("1")
        }

        val cursor = writableDatabase.query(
                CACHED_LESSON_TYPES_TABLE, projection, where, selectionArgs, null, null, null
        )

        val periods = ArrayList<CachedLessonType>()
        while (cursor.moveToNext()) {
            periods.add(CachedLessonType.fromLessonTypeCursor(cursor))
        }
        cursor.close()

        return periods
    }

    private fun loadCachedLessonsIntersectingInterval(
            cachedPeriod: CachedPeriod,
            interval: WeekInterval): ArrayList<CachedLesson> {
        val projection = arrayOf(CL_CACHED_PERIOD_ID, CL_LESSON_ID, CL_STARTS_AT_MS, CL_FINISHES_AT_MS, CL_TEACHING_ID, CL_WEEK_NUMBER, CL_YEAR, CL_SUBJECT, CL_ROOM, CL_DESCRIPTION, CL_COLOR)

        val intervalToIntersect = interval.toCalendarInterval()

        val selection: String
        val selectionArgs: Array<String>
        if (cachedPeriod.cachedPeriodType == null) {
            selection = CL_CACHED_PERIOD_ID + " = ? " +
                    "AND " + CL_STARTS_AT_MS + " >= ? AND " + CL_FINISHES_AT_MS + " <= ?"
            selectionArgs = arrayOf(cachedPeriod.id.toString(), intervalToIntersect.fromMs.toString(), intervalToIntersect.toMs.toString())
        } else {
            //it's an extra course: we load only it's lessons
            selection = CL_CACHED_PERIOD_ID + " = ? AND " + CL_TEACHING_ID + " = ? " +
                    "AND " + CL_STARTS_AT_MS + " >= ? AND " + CL_FINISHES_AT_MS + " <= ?"
            selectionArgs = arrayOf(cachedPeriod.id.toString(), cachedPeriod.cachedPeriodType.toString(), intervalToIntersect.fromMs.toString(), intervalToIntersect.toMs.toString())
        }

        val cursor = writableDatabase.query(
                CACHED_LESSONS_TABLE, projection, selection, selectionArgs, null, null, null
        )

        val lessons = getCachedLessonsFromCursor(cursor)
        cursor.close()

        return lessons
    }

    private fun getCachedLessonsFromCursor(cursor: Cursor): ArrayList<CachedLesson> {
        val periods = ArrayList<CachedLesson>()
        while (cursor.moveToNext()) {
            periods.add(CachedLesson.fromCursor(cursor))
        }
        return periods
    }

    /**
     * Deletes EVERYTHING about the current study course from the cache.
     */
    fun purgeStudyCourseCache() = jobQueue.addJobInBackground(PurgeStudyCourseCacheJob())

    private val idsOfCachedPeriodsOfStudyCourses: ArrayList<Long>
        get() = queryForCachedPeriodIds(CP_PERIOD_TYPE + " IS NULL")

    private fun getIdsOfCachedPeriodsWithLessonType(lessonTypeId: String): ArrayList<Long> =
            queryForCachedPeriodIds(CP_PERIOD_TYPE + " = " + lessonTypeId.querify())

    private fun queryForCachedPeriodIds(where: String): ArrayList<Long> {
        val projection = arrayOf(CP_ID)
        val query = writableDatabase.query(CACHED_PERIOD_TABLE, projection, where, null, null, null, null)

        val ids = ArrayList<Long>()
        while (query.moveToNext()) {
            ids.add(query.getLong(0))
        }

        query.close()

        return ids
    }


    private fun deleteAllLessonTypes(keepExtras: Boolean) {
        var whereClause: String? = null
        if (keepExtras) {
            whereClause = CLT_IS_EXTRA_COURSE + " <> 1"
        }

        writableDatabase.delete(CACHED_LESSON_TYPES_TABLE, whereClause, null)
    }

    fun removeExtraCoursesWithLessonType(lessonTypeId: String) =
            jobQueue.addJobInBackground(RemoveExtraCoursesWithLessonType(lessonTypeId))

    /**
     * Deletes EVERYTHING from the case.
     */
    fun obliterateAllLessonsCache() = jobQueue.addJobInBackground(ObliterateLessonsCacheJob())

    fun getNotCachedSubintervals(
            interval: WeekInterval,
            courses: ArrayList<ExtraCourse>,
            listener: (ArrayList<NotCachedInterval>) -> Unit) =
            jobQueue.addJobInBackground(GetNotCachedSubintervalsJob(interval, courses, listener))

    /**
     * Fetches all the lessons planned for today; the lessons are ordered by start ms.
     */
    fun getTodaysCachedLessons(listener: TodaysLessonsListener) =
            jobQueue.addJobInBackground(GetTodaysLessonsJob(listener))

    /**
     * @param day the day to check
     * @param listener the callback. Note that this callback will be positive only when the
     * study course and all the extra courses are cached.
     */
    fun isDayCached(
            day: WeekDayTime,
            listener: IsDayCachedListener) =
            jobQueue.addJobInBackground(IsDayCachedJob(day, listener))

    private fun doDuplicatedRecordsExist(): Boolean {
        val cursor = writableDatabase.rawQuery(
                "select count(*) AS duplicated_rows FROM ( " +
                        "select lesson_id, count(*) " +
                        "from cached_lessons " +
                        "group by lesson_id " +
                        "  having count(*) > 1 " +
                        ")", null
        )

        val numDuplicatedRows: Int
        numDuplicatedRows = if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndex("duplicated_rows"))
        } else {
            0
        }

        cursor.close()

        return numDuplicatedRows != 0
    }

    private fun removeExtraCoursesWithLessonTypeImpl(lessonTypeId: String) {
        for (cachedExtraId in getIdsOfCachedPeriodsWithLessonType(lessonTypeId)) {
            deleteLessonTypeWithId(lessonTypeId)
            deleteExtraLessonsOfType(cachedExtraId, lessonTypeId)
            deleteCachedPeriodWithId(cachedExtraId)
        }
    }

    fun removeExtraCoursesNotInList(extraCoursesToKeep: ExtraCoursesList) {
        for (lessonTypeToDelete in findExtraLessonTypesNotInList(extraCoursesToKeep)) {
            removeExtraCoursesWithLessonTypeImpl(lessonTypeToDelete.lesson_type_id)
        }
    }

    private fun findExtraLessonTypesNotInList(extraCoursesToKeep: ExtraCoursesList): ArrayList<CachedLessonType> {
        val lessonTypesToDelete = ArrayList<CachedLessonType>()
        for (cachedLessonType in loadExtraCoursesLessonTypes()) {
            var isLessonToKeep = false
            for (courseToKeep in extraCoursesToKeep) {
                if (cachedLessonType.lesson_type_id == courseToKeep.lessonTypeId) {
                    isLessonToKeep = true
                }
            }

            if (!isLessonToKeep) {
                lessonTypesToDelete.add(cachedLessonType)
            }
        }
        return lessonTypesToDelete
    }

    /**
     * Saves in the cache the opening times of the library. The times are considered to be fresh
     * and fetched right now.<br></br>
     * In case there is an already cached version
     * @param openingTimes the times to cached.
     */
    fun cacheLibraryOpeningTimes(openingTimes: LibraryOpeningTimes) =
            jobQueue.addJobInBackground(CacheLibraryOpeningTimesJob(openingTimes))

    fun getCachedLibraryOpeningTimes(
            day: Calendar,
            fetchDeadCacheToo: Boolean,
            listener: CachedLibraryOpeningTimesListener) =
            jobQueue.addJobInBackground(CachedLibraryOpeningTimesJob(day, fetchDeadCacheToo, listener))

    /**
     * @return the unix timestamp before which the cache of the opening times of the library is
     * considered to be old.
     */
    private val lastValidLibraryCachePeriod: Long
        get() {
            val lastValid = CalendarUtils.debuggableToday
            lastValid.add(Calendar.DAY_OF_WEEK, -5)

            return lastValid.timeInMillis
        }

    private fun buildLibraryOpeningTimesFromCursor(cursor: Cursor): LibraryOpeningTimes {
        val times = LibraryOpeningTimes()
        times.day = cursor.getString(cursor.getColumnIndex(CLIBT_DAY))
        times.timesBuc = cursor.getString(cursor.getColumnIndex(CLIBT_BUC))
        times.timesCial = cursor.getString(cursor.getColumnIndex(CLIBT_CIAL))
        times.timesMesiano = cursor.getString(cursor.getColumnIndex(CLIBT_MESIANO))
        times.timesPovo = cursor.getString(cursor.getColumnIndex(CLIBT_POVO))
        times.timesPsicologia = cursor.getString(cursor.getColumnIndex(CLIBT_PSICOLOGIA))
        return times
    }

    internal abstract inner class CacheJob protected constructor() : Job(Params(PRIORITY_NORMAL)) {

        override fun onAdded() = Unit

        override fun onCancel(
                cancelReason: Int,
                throwable: Throwable?) = Unit

        override fun shouldReRunOnThrowable(
                throwable: Throwable,
                runCount: Int,
                maxRunCount: Int): RetryConstraint {
            throwable.printStackTrace()
            BugLogger.logBug("Throwable raised when running a job", throwable)

            return RetryConstraint.CANCEL
        }
    }

    internal inner class GetLessonsInFreshOrDeadCacheJob internal constructor(
            private val intervalToLoad: WeekInterval,
            private val extraCourses: ArrayList<ExtraCourse>,
            private val fetchOldCache: Boolean,
            private val listener: (CachedLessonsSet) -> Unit) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() =
                listener(getLessonsInFreshOrDeadCache(intervalToLoad, extraCourses, fetchOldCache))
    }

    internal inner class CacheExtraLessonsSetJob internal constructor(
            setToCache: LessonsSet,
            private val interval: WeekInterval,
            private val extraCourse: ExtraCourse) : CacheJob() {

        private val lessonTypesToCache: ArrayList<LessonType> = ArrayList(setToCache.lessonTypes.values)

        //Since the lessons set is modifiable, it's better to create a copy of what we want to
        //save before it get's changed in some way. This fixes #42 and #37
        private val lessonsToCache: ArrayList<LessonSchedule> = ArrayList(setToCache.scheduledLessons.values)

        @Throws(Throwable::class)
        override fun onRun() {

            updateLessonTypesFromSet(true, lessonTypesToCache)

            deleteCachedExtraLessonsInInterval(interval, extraCourse)

            val cachedPeriodId = insertCachedPeriod(CachedPeriod(interval, extraCourse.lessonTypeId))
            for (lesson in lessonsToCache) {
                cacheLesson(CachedLesson(cachedPeriodId, lesson))
            }

            if (Config.DEBUG_MODE && doDuplicatedRecordsExist()) {
                UIUtils.showToastIfInDebug(applicationContext, "DUPLICATE RECORDS FOUND!")
            }

        }
    }

    internal inner class CacheLessonsSetJob internal constructor(
            setToCache: LessonsSet,
            private val intervalToCache: WeekInterval) : CacheJob() {

        private val lessonTypesToCache: ArrayList<LessonType> = ArrayList(setToCache.lessonTypes.values)
        private val lessonsToCache: ArrayList<LessonSchedule> = ArrayList(setToCache.scheduledLessons.values)

        init {
            //Since the lessons set is modifiable, it's better to create a copy of what we want to
            //save before it get's changed in some way. This fixes #42 and #37
        }

        @Throws(Throwable::class)
        override fun onRun() {
            //0) Overwrite existing lesson types
            //1) Clear the already existing lessons cached in the given interval
            //2) Add the new cache period
            //3) Get inserted id
            //4) Make lessons
            //5) Add lessons
            updateLessonTypesFromSet(false, lessonTypesToCache)
            deleteCachedStudyCourseLessonsInInterval(intervalToCache)

            val cachedPeriodId = insertCachedPeriod(CachedPeriod(intervalToCache))
            for (lesson in lessonsToCache) {
                cacheLesson(CachedLesson(cachedPeriodId, lesson))
            }

            if (Config.DEBUG_MODE && doDuplicatedRecordsExist()) {
                UIUtils.showToastIfInDebug(applicationContext, "DUPLICATE RECORDS FOUND!")
            }
        }

    }

    internal inner class PurgeStudyCourseCacheJob : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() {
            for (studyCourseCachedPeriodsId in idsOfCachedPeriodsOfStudyCourses) {
                deleteStudyCourseLessons(studyCourseCachedPeriodsId)
                deleteCachedPeriodWithId(studyCourseCachedPeriodsId)
            }

            deleteAllLessonTypes(true)
        }

    }

    internal inner class RemoveExtraCoursesWithLessonType internal constructor(private val lessonTypeId: String) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() = removeExtraCoursesWithLessonTypeImpl(lessonTypeId)
    }

    internal inner class ObliterateLessonsCacheJob : CacheJob() {
        @Throws(Throwable::class)
        override fun onRun() {
            writableDatabase.delete(CACHED_PERIOD_TABLE, null, null)
            writableDatabase.delete(CACHED_LESSONS_TABLE, null, null)
            writableDatabase.delete(CACHED_LESSON_TYPES_TABLE, null, null)
        }
    }

    internal inner class GetNotCachedSubintervalsJob internal constructor(
            private val interval: WeekInterval,
            private val courses: ArrayList<ExtraCourse>,
            val listener: (ArrayList<NotCachedInterval>) -> Unit) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() =
                listener(getLessonsInFreshOrDeadCache(interval, courses, false).missingIntervals)
    }

    internal inner class GetTodaysLessonsJob internal constructor(
            private val listener: TodaysLessonsListener
    ) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() {
            val now = CalendarUtils.debuggableToday
            now.set(Calendar.HOUR_OF_DAY, 0)
            now.set(Calendar.MINUTE, 0)
            now.set(Calendar.SECOND, 0)

            val endOfDay = now.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)


            val lessons = cachedLessonsToLessonSchedule(
                    getLessonsInPeriod(now.timeInMillis, endOfDay.timeInMillis))

            listener.onLessonsAvailable(lessons)
        }


    }

    internal inner class IsDayCachedJob internal constructor(
            private val today: WeekDayTime,
            private val listener: IsDayCachedListener) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() {
            val thisWeekInterval = today.containingInterval

            //Is study course cached
            if (isStudyCourseCached(thisWeekInterval)) {
                listener.onIsCachedResult(areExtraCoursesCached(thisWeekInterval))
            } else {
                listener.onIsCachedResult(false)
            }
        }

        private fun areExtraCoursesCached(thisWeekInterval: WeekInterval): Boolean {
            for (extraCourse in AppPreferences.extraCourses) {
                val lessonTypeId = extraCourse.lessonTypeId
                if (loadExtraCourseCachePeriods(thisWeekInterval, true, lessonTypeId).isEmpty()) {
                    return false
                }
            }

            return true
        }

        private fun isStudyCourseCached(thisWeekInterval: WeekInterval): Boolean =
                !loadStudyCourseCachePeriods(thisWeekInterval, true).isEmpty()

    }

    internal inner class CacheLibraryOpeningTimesJob internal constructor(private val openingTimes: LibraryOpeningTimes) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() {
            val values = ContentValues()
            values.put(CLIBT_DAY, openingTimes.day)
            values.put(CLIBT_BUC, openingTimes.timesBuc)
            values.put(CLIBT_CIAL, openingTimes.timesCial)
            values.put(CLIBT_MESIANO, openingTimes.timesMesiano)
            values.put(CLIBT_POVO, openingTimes.timesPovo)
            values.put(CLIBT_PSICOLOGIA, openingTimes.timesPsicologia)
            values.put(CLIBT_CACHED_IN_MS, CalendarUtils.debuggableMillis)

            writableDatabase.replace(CACHED_LIBRARY_TIMES_TABLE_NAME, null, values)
        }

    }

    internal inner class CachedLibraryOpeningTimesJob internal constructor(
            private val day: Calendar,
            private val fetchDeadCacheToo: Boolean,
            private val listener: CachedLibraryOpeningTimesListener) : CacheJob() {

        @Throws(Throwable::class)
        override fun onRun() {
            //Querying
            val columns = arrayOf(CLIBT_DAY, CLIBT_BUC, CLIBT_CIAL, CLIBT_MESIANO, CLIBT_POVO, CLIBT_PSICOLOGIA)
            val selection = "$CLIBT_DAY = ? AND $CLIBT_CACHED_IN_MS >= ?"

            val lastValidMs = java.lang.Long.toString(if (fetchDeadCacheToo) -1 else lastValidLibraryCachePeriod)
            val selectionArgs = arrayOf(LibraryOpeningTimes.formatDay(day), lastValidMs)

            val cursor = writableDatabase.query(
                    CACHED_LIBRARY_TIMES_TABLE_NAME, columns, selection, selectionArgs, null, null, null
            )

            if (cursor.moveToFirst()) {

                listener.onCachedOpeningTimesFound(buildLibraryOpeningTimesFromCursor(cursor))
            } else {
                listener.onNoCachedOpeningTimes()
            }

            cursor.close()
        }

    }

    companion object {

        val FRESH_CACHE_DURATION_MS = 1000 * 60 * 60 * 24 * 7

        val PRIORITY_NORMAL = 1

        private fun buildWeekTimeSelectionsSQL(period: WeekInterval): String =
                buildWeekTimeSelectionsSQL(period, CL_WEEK_NUMBER, CL_YEAR)

        private fun buildWeekTimeSelectionsSQL(
                interval: WeekInterval,
                weekName: String,
                yearName: String): String {
            val weeksPieces = ArrayList<String>()
            val weeksIterator = interval.iterator
            while (weeksIterator.hasNext()) {
                val week = weeksIterator.next()

                val piece = String.format(
                        Locale.UK, "(%s=%d AND %s=%d)", weekName, week.weekNumber, yearName, week.year
                )
                weeksPieces.add(piece)
            }

            return StringUtils.implode(weeksPieces, " OR ")
        }
    }

}

private fun String.querify(): String = "\"$this\""
