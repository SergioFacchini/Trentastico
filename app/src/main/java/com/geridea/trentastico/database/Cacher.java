package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLesson;
import com.geridea.trentastico.model.cache.CachedLessonType;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.CachedPeriod;
import com.geridea.trentastico.utils.StringUtils;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekIntervalCutResult;
import com.geridea.trentastico.utils.time.WeekTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * The class that deals with the caching of lessons.<br>
 * There are 3 types o caches: <br>
 * <ul>
 *     <li><strong>Fresh cache: </strong> the cache that has just been fetched and didn't expire
 *     yet.</li>
 *     <li><strong>Old cache: </strong> the cache that has expired but still can be used if unable
 *     to get the lesson from the network.</li>
 *     <li><strong>Dead cache: </strong> the cache that we saved relatively to past period.
 *     Technically this kind of cache will never get updated, and it will lie in our cache database
 *     indefinitely. </li>
 * </ul>
 */
public class Cacher {

    public static final int FRESH_CACHE_DURATION_MS = 1000 * 60 * 60 * 24 * 7;

    //Cached periods
    public static final String CACHED_PERIOD_TABLE = "cached_periods";

    public static final String CP_ID           = "id";
    public static final String CP_START_WEEK   = "start_week";
    public static final String CP_START_YEAR   = "start_year";
    public static final String CP_END_WEEK     = "end_week";
    public static final String CP_END_YEAR     = "end_year";
    public static final String CP_LESSON_TYPE  = "lesson_type";
    public static final String CP_CACHED_IN_MS = "cached_in_ms";

    static final String SQL_CREATE_CACHED_PERIOD =
        "CREATE TABLE "+ CACHED_PERIOD_TABLE+" (" +
            CP_ID +           " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            CP_START_WEEK +   " INTEGER NOT NULL, " +
            CP_START_YEAR +   " INTEGER NOT NULL, " +
            CP_END_WEEK +     " INTEGER NOT NULL, " +
            CP_END_YEAR +     " INTEGER NOT NULL, " +
            CP_LESSON_TYPE +  " INTEGER NOT NULL, " +
            CP_CACHED_IN_MS + " INTEGER NOT NULL" +
        ")";



    //Cached lessons
    public static final String CACHED_LESSONS_TABLE = "cached_lessons";

    public static final String CL_CACHED_PERIOD_ID = "cached_period_id";
    public static final String CL_LESSON_ID        = "lesson_id";
    public static final String CL_STARTS_AT_MS     = "starts_at_ms";
    public static final String CL_FINISHES_AT_MS   = "finishes_at_ms";
    public static final String CL_TEACHING_ID      = "teaching_id";
    public static final String CL_WEEK_NUMBER      = "week_number";
    public static final String CL_YEAR             = "year";
    public static final String CL_SUBJECT          = "subject";
    public static final String CL_ROOM             = "room";
    public static final String CL_DESCRIPTION      = "description";
    public static final String CL_COLOR            = "color";

    static final String SQL_CREATE_CACHED_LESSONS =
        "CREATE TABLE " + CACHED_LESSONS_TABLE + " (" +
                CL_CACHED_PERIOD_ID + " INTEGER NOT NULL," +
                CL_LESSON_ID        + " INTEGER NOT NULL, " +
                CL_STARTS_AT_MS     + " INTEGER NOT NULL, " +
                CL_FINISHES_AT_MS   + " INTEGER NOT NULL, " +
                CL_TEACHING_ID      + " INTEGER NOT NULL, " +
                CL_WEEK_NUMBER      + " INTEGER NOT NULL, " +
                CL_YEAR             + " INTEGER NOT NULL, " +
                CL_SUBJECT          + " VARCHAR(500) NOT NULL, " +
                CL_ROOM             + " VARCHAR(500) NOT NULL, " +
                CL_DESCRIPTION      + " VARCHAR(500) NOT NULL, " +
                CL_COLOR            + " INTEGER NOT NULL " +
            ")";


    //Cached lesson types
    public static final String CACHED_LESSON_TYPES_TABLE = "cached_lesson_types";

    public static final String CLT_LESSON_TYPE_ID = "lesson_type_id";
    public static final String CLT_NAME = "name";
    public static final String CLT_COLOR = "color";
    public static final String CLT_IS_EXTRA_COURSE = "is_extra_course";

    static final String SQL_CREATE_CACHED_LESSON_TYPES =
        "CREATE TABLE " + CACHED_LESSON_TYPES_TABLE + " (" +
            CLT_LESSON_TYPE_ID +  " INTEGER NOT NULL, " +
            CLT_NAME +            " VARCHAR(500) NOT NULL, " +
            CLT_COLOR +           " INTEGER NOT NULL," +
            CLT_IS_EXTRA_COURSE + " INTEGER NOT NULL" +
        ")";

    private static SQLiteDatabase writableDatabase;

    public static void init(Context context){
        CacheDbHelper cacheDbHelper = new CacheDbHelper(context);
        writableDatabase = cacheDbHelper.getWritableDatabase();
    }

    public static void cacheExtraLessonsSet(LessonsSet setToCache, WeekInterval interval, ExtraCourse extraCourse) {
        updateLessonTypesFromSet(setToCache, true);
        deleteCachedExtraLessonsInInterval(interval, extraCourse);

        long cachedPeriodId = insertCachedPeriod(new CachedPeriod(interval, extraCourse.getLessonTypeId()));
        for (LessonSchedule lesson : setToCache.getScheduledLessons()) {
            cacheLesson(new CachedLesson(cachedPeriodId, lesson));
        }
    }

    public static void cacheLessonsSet(LessonsSet setToCache, WeekInterval intervalToCache) {
        //0) Overwrite existing lesson types
        //1) Clear the already existing lessons cached in the given interval
        //2) Add the new cache period
        //3) Get inserted id
        //4) Make lessons
        //5) Add lessons
        updateLessonTypesFromSet(setToCache, false);
        deleteCachedStudyCourseLessonsInInterval(intervalToCache);

        long cachedPeriodId = insertCachedPeriod(new CachedPeriod(intervalToCache));
        for (LessonSchedule lesson : setToCache.getScheduledLessons()) {
            cacheLesson(new CachedLesson(cachedPeriodId, lesson));
        }
    }

    private static void updateLessonTypesFromSet(LessonsSet setToCache, boolean containsExtraLessonTypes) {
        for (LessonType lessonType : setToCache.getLessonTypes()) {
            LessonSchedule lesson = setToCache.getALessonHavingType(lessonType);
            if (lesson != null) {
                deleteLessonTypeWithId(lessonType.getId());
                cacheLessonType(new CachedLessonType(lessonType), containsExtraLessonTypes);
            }
        }
    }

    private static void cacheLessonType(CachedLessonType cachedLessonType, boolean isExtraCourse) {
        ContentValues values = new ContentValues();
        values.put(CLT_LESSON_TYPE_ID,  cachedLessonType.getLesson_type_id());
        values.put(CLT_NAME,            cachedLessonType.getName());
        values.put(CLT_COLOR,           cachedLessonType.getColor());
        values.put(CLT_IS_EXTRA_COURSE, isExtraCourse);

        writableDatabase.insert(CACHED_LESSON_TYPES_TABLE, null, values);
    }

    private static void deleteLessonTypeWithId(int lessonTypeId) {
        String selection = CLT_LESSON_TYPE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(lessonTypeId) };

        writableDatabase.delete(CACHED_LESSON_TYPES_TABLE, selection, selectionArgs);
    }

    private static void cacheLesson(CachedLesson cachedLesson) {
        ContentValues values = new ContentValues();
        values.put(CL_CACHED_PERIOD_ID, cachedLesson.getCached_period_id());
        values.put(CL_LESSON_ID,        cachedLesson.getLesson_id());
        values.put(CL_STARTS_AT_MS,     cachedLesson.getStarts_at_ms());
        values.put(CL_FINISHES_AT_MS,   cachedLesson.getFinishes_at_ms());
        values.put(CL_WEEK_NUMBER,      cachedLesson.getWeekTime().getWeekNumber());
        values.put(CL_YEAR,             cachedLesson.getWeekTime().getYear());
        values.put(CL_TEACHING_ID,      cachedLesson.getTeaching_id());
        values.put(CL_SUBJECT,          cachedLesson.getSubject());
        values.put(CL_ROOM,             cachedLesson.getRoom());
        values.put(CL_DESCRIPTION,      cachedLesson.getDescription());
        values.put(CL_COLOR,            cachedLesson.getColor());

        writableDatabase.insert(CACHED_LESSONS_TABLE, null, values);
    }

    private static void deleteCachedExtraLessonsInInterval(WeekInterval interval, ExtraCourse extraCourse) {
        //Trimming existing cached periods so the passed interval won't have any overlapping
        //cached interval
        for (CachedPeriod period: loadExtraCourseCachePeriods(interval, true, extraCourse.getLessonTypeId())) {
            WeekIntervalCutResult cutResult = period.getPeriod().cutFromInterval(interval);

            if(!cutResult.hasAnyRemainingResult()){
                deleteCachedPeriodWithId(period.getId());
            } else if(cutResult.hasOnlyOneResult()){
                period.setPeriod(cutResult.getFirstRemaining());
                updateCachedPeriod(period);
            } else {
                splitCachePeriod(period, cutResult);
            }

            deleteExtraCourseLessonsInInterval(period.getId(), interval, extraCourse.getLessonTypeId());
        }

    }

    private static void deleteCachedStudyCourseLessonsInInterval(WeekInterval interval) {
        //Trimming existing cached periods so the passed interval won't have any overlapping
        //cached interval
        for (CachedPeriod period: loadStudyCourseCachePeriods(interval, true)) {
            WeekIntervalCutResult cutResult = period.getPeriod().cutFromInterval(interval);

            if(!cutResult.hasAnyRemainingResult()){
                deleteCachedPeriodWithId(period.getId());
            } else if(cutResult.hasOnlyOneResult()){
                period.setPeriod(cutResult.getFirstRemaining());
                updateCachedPeriod(period);
            } else {
                splitCachePeriod(period, cutResult);
            }

            deleteStudyCourseLessonsInInterval(period.getId(), interval);
        }
    }

    /**
     * Splits the cached period in two periods. This means that all the schedules associated to
     * the cache period will be adapted to the new periods; this will generate new instances of
     * cache periods in the database and re-associate the the cached lessons accordingly.<br>
     * WARNING: this method does NOT deletes the cached lessons that will be left unassociated due
     * to the split! These will be left in the database associated to the original id. It's the duty
     * of the caller to delete these records.
     */
    private static void splitCachePeriod(CachedPeriod originalPeriod, WeekIntervalCutResult cutResult) {
        originalPeriod.setPeriod(cutResult.getFirstRemaining());
        updateCachedPeriod(originalPeriod);

        CachedPeriod newPeriod = originalPeriod.copy();
        newPeriod.setPeriod(cutResult.getSecondRemaining());
        insertCachedPeriod(newPeriod);

        associateCachedLessonsToAnotherPeriod(originalPeriod.getId(), newPeriod.getId(), newPeriod.getPeriod());
    }

    private static void associateCachedLessonsToAnotherPeriod(long originalPeriodId, long newPeriodId, WeekInterval periodToMove) {
        if (periodToMove.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot reassociate cached lessons to an empty period!"
            );
        }

        ContentValues values = new ContentValues();
        values.put(CL_CACHED_PERIOD_ID, newPeriodId);

        String selection = String.format(CL_CACHED_PERIOD_ID + " = ? AND (%s)", buildWeekTimeSelectionsSQL(periodToMove));
        String[] selectionArgs = { String.valueOf(originalPeriodId) };

        writableDatabase.update(CACHED_LESSONS_TABLE, values, selection, selectionArgs);
    }

    @NonNull
    private static String buildWeekTimeSelectionsSQL(WeekInterval period) {
        return buildWeekTimeSelectionsSQL(period, CL_WEEK_NUMBER, CL_YEAR);
    }

    private static String buildWeekTimeSelectionsSQL(WeekInterval interval, String weekName, String yearName) {
        ArrayList<String> weeksPieces = new ArrayList<>();
        Iterator<WeekTime> weeksIterator = interval.getIterator();
        while(weeksIterator.hasNext()){
            WeekTime week = weeksIterator.next();

            String piece = String.format(
                    Locale.UK, "(%s=%d AND %s=%d)", weekName, week.getWeekNumber(), yearName, week.getYear()
            );
            weeksPieces.add(piece);
        }

        return StringUtils.implode(weeksPieces, " OR ");
    }

    private static void updateCachedPeriod(CachedPeriod period) {
        ContentValues values = getCachedPeriodContentValues(period);
        values.put(CP_ID, period.getId());

        // Which row to update, based on the title
        String selection = CP_ID+" = ?";
        String[] selectionArgs = { String.valueOf(period.getId()) };

        writableDatabase.update(CACHED_PERIOD_TABLE, values, selection, selectionArgs);
    }

    @NonNull
    private static ContentValues getCachedPeriodContentValues(CachedPeriod period) {
        ContentValues values = new ContentValues();
        values.put(CP_START_WEEK,   period.getPeriod().getStartWeekNumber());
        values.put(CP_START_YEAR,   period.getPeriod().getStartYear());
        values.put(CP_END_WEEK,     period.getPeriod().getEndWeekNumber());
        values.put(CP_END_YEAR,     period.getPeriod().getEndYear());
        values.put(CP_LESSON_TYPE,  period.getLesson_type());
        values.put(CP_CACHED_IN_MS, period.getCached_in_ms());
        return values;
    }

    private static void deleteCachedPeriodWithId(long periodId) {
        String[] selectionArgs = { String.valueOf(periodId) };
        writableDatabase.delete(CACHED_PERIOD_TABLE, CP_ID+"= ?", selectionArgs);
    }

    private static void deleteExtraCourseLessonsInInterval(long periodId, WeekInterval interval, int lessonTypeId) {
        deleteCourseLessonsInIntervalInternal(periodId, interval, lessonTypeId);
    }

    private static void deleteCourseLessonsInIntervalInternal(long periodId, @Nullable WeekInterval interval, long lessonTypeId) {
        String whereClause = String.format("(%s = ?) ", CL_CACHED_PERIOD_ID);

        if (interval != null) {
            whereClause += String.format(" AND (%s) ", buildWeekTimeSelectionsSQL(interval));
        }

        if (lessonTypeId != 0) {
            whereClause += "AND "+CL_TEACHING_ID+" = "+lessonTypeId;
        }

        String[] whereArgs = new String[]{ String.valueOf(periodId) };
        writableDatabase.delete(CACHED_LESSONS_TABLE, whereClause, whereArgs);
    }

    private static void deleteStudyCourseLessonsInInterval(long periodId, WeekInterval interval) {
        deleteCourseLessonsInIntervalInternal(periodId, interval, 0);
    }

    private static void deleteStudyCourseLessons(long periodId) {
        deleteCourseLessonsInIntervalInternal(periodId, null, 0);
    }

    private static void deleteExtraLessonsOfType(long periodId, long lessonTypeId) {
        deleteCourseLessonsInIntervalInternal(periodId, null, lessonTypeId);
    }

    private static ArrayList<CachedPeriod> loadStudyCourseCachePeriods(WeekInterval intervalToLoad, boolean fetchOldCacheToo) {
        return loadCachePeriodsInternal(intervalToLoad, fetchOldCacheToo, 0);
    }

    private static ArrayList<CachedPeriod> loadExtraCourseCachePeriods(WeekInterval intervalToLoad, boolean fetchOldCacheToo, int lessonTypeId) {
        return loadCachePeriodsInternal(intervalToLoad, fetchOldCacheToo, lessonTypeId);
    }

    @NonNull
    private static ArrayList<CachedPeriod> loadCachePeriodsInternal(WeekInterval intervalToLoad, boolean fetchOldCacheToo, int lessonTypeId) {
        String[] projection = {
                CP_ID,
                CP_START_WEEK,
                CP_START_YEAR,
                CP_END_WEEK,
                CP_END_YEAR,
                CP_LESSON_TYPE,
                CP_CACHED_IN_MS
        };

        String sqlCachePeriods = buildCachePeriodSelectionForInterval(intervalToLoad);
        String selection = String.format("(%s) AND %s = %d ", sqlCachePeriods, CP_LESSON_TYPE, lessonTypeId);
        if(!fetchOldCacheToo){
            selection += String.format(
                    " AND (%s >= %d AND %s < %d) ",
                    CP_CACHED_IN_MS, getLastValidCacheMs(), CP_CACHED_IN_MS, System.currentTimeMillis()
            );
        }

        Cursor cursor = writableDatabase.query(
            CACHED_PERIOD_TABLE, projection, selection, null, null, null, null
        );

        ArrayList<CachedPeriod> periods = new ArrayList<>();
        while(cursor.moveToNext()) {
            periods.add(CachedPeriod.fromCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    @NonNull
    private static String buildCachePeriodSelectionForInterval(WeekInterval intervalToLoad) {
        WeekTime startFrom = intervalToLoad.getStartCopy();
        WeekTime startTo   = intervalToLoad.getEndCopy();
        WeekInterval intervalContainingStart = new WeekInterval(startFrom, startTo);

        WeekTime endFrom = intervalToLoad.getStartCopy();
        endFrom.addWeeks(1);
        WeekTime endTo   = intervalToLoad.getEndCopy();
        WeekInterval intervalContainingEnd = new WeekInterval(endFrom, endTo);

        String startStr = buildWeekTimeSelectionsSQL(intervalContainingStart, CP_START_WEEK, CP_START_YEAR);
        String endStr   = buildWeekTimeSelectionsSQL(intervalContainingEnd,   CP_END_WEEK,   CP_END_YEAR);

        String expression = startStr;
        if(!endStr.isEmpty()){ //Can happen when fetching a one-week interval
            expression +=  " OR "+ endStr;
        }
        return expression;
    }

    private static long getLastValidCacheMs() {
        return System.currentTimeMillis() - FRESH_CACHE_DURATION_MS;
    }

    private static long insertCachedPeriod(CachedPeriod cachedPeriod) {
        ContentValues values = getCachedPeriodContentValues(cachedPeriod);

        long id = writableDatabase.insert(CACHED_PERIOD_TABLE, null, values);
        cachedPeriod.setId(id);

        return id;
    }

    public static CachedLessonsSet getLessonsInFreshOrDeadCache(WeekInterval intervalToLoad, ArrayList<ExtraCourse> extraCourses, boolean fetchOldCacheToo) {
        CachedLessonsSet lessonsSet = new CachedLessonsSet();

        //Lesson types
        lessonsSet.addCachedLessonTypes(loadLessonTypes(true, false));

        //Study course cached periods
        ArrayList<CachedPeriod> studyCoursePeriods = loadStudyCourseCachePeriods(intervalToLoad, fetchOldCacheToo);
        for (CachedPeriod cachedPeriod : studyCoursePeriods) {
            lessonsSet.addLessonSchedules(loadLessonsOfCachePeriod(cachedPeriod));
            lessonsSet.addCachedPeriod(cachedPeriod);
        }

        //Missing study course intervals
        lessonsSet.addMissingIntervals(
            toStudyCourseInterval(findMissingIntervalsInCachePeriods(intervalToLoad, studyCoursePeriods))
        );

        ////////////////
        //Extra courses
        lessonsSet.addCachedLessonTypes(loadExtraCoursesLessonTypes());

        //Calculating missing intervals for all the extra study courses
        for (ExtraCourse extraCourse : extraCourses) {
            //Loading cached periods
            ArrayList<CachedPeriod> extraPeriods = loadExtraCourseCachePeriods(
                    intervalToLoad, fetchOldCacheToo, extraCourse.getLessonTypeId()
            );

            //Adding to lessons' set what's cached
            for (CachedPeriod cachedPeriod: extraPeriods) {
                lessonsSet.addLessonSchedules(loadLessonsOfCachePeriod(cachedPeriod));
                lessonsSet.addCachedPeriod(cachedPeriod);
            }

            //Finding not cached extra intervals
            ArrayList<WeekInterval> missingIntervals =
                    findMissingIntervalsInCachePeriods(intervalToLoad, extraPeriods);
            lessonsSet.addMissingIntervals(toExtraCourseInterval(missingIntervals, extraCourse));
        }
        
        lessonsSet.recalculatePartitionings();

        return lessonsSet;
    }

    private static ArrayList<CachedLessonType> loadExtraCoursesLessonTypes() {
        return loadLessonTypes(false, true);
    }

    public static ArrayList<NotCachedInterval> toStudyCourseInterval(List<WeekInterval> intervals){
        ArrayList<NotCachedInterval> notCachedIntervals = new ArrayList<>();
        for (WeekInterval interval : intervals) {
            notCachedIntervals.add(new StudyCourseNotCachedInterval(interval));
        }

        return notCachedIntervals;
    }

    public static ArrayList<NotCachedInterval> toExtraCourseInterval(List<WeekInterval> intervals, ExtraCourse extraCourse){
        ArrayList<NotCachedInterval> notCachedIntervals = new ArrayList<>();
        for (WeekInterval interval : intervals) {
            notCachedIntervals.add(new ExtraCourseNotCachedInterval(interval, extraCourse));
        }

        return notCachedIntervals;
    }

    private static ArrayList<LessonSchedule> loadLessonsOfCachePeriod(CachedPeriod cachePeriod) {
        ArrayList<LessonSchedule> lessons = new ArrayList<>();

        for (CachedLesson cachedLesson : loadCachedLessons(cachePeriod)) {
            lessons.add(new LessonSchedule(cachedLesson));
        }

        return lessons;
    }

    @NonNull
    private static ArrayList<WeekInterval> findMissingIntervalsInCachePeriods(WeekInterval intervalToLoad, ArrayList<CachedPeriod> cachePeriods) {
        ArrayList<WeekInterval> missingIntervals = new ArrayList<>();

        boolean isMissingIntervalBeingBuild = false;
        WeekTime missingStart = null;
        WeekTime missingEnd = null;

        Iterator<WeekTime> iterator = intervalToLoad.getIterator();
        while (iterator.hasNext()){
            WeekTime timeToCheck = iterator.next();

            //true if there is a cached period that contains the week time
            boolean wasCachedPeriodFound = false;
            for (CachedPeriod cachePeriod : cachePeriods) {
                if (cachePeriod.contains(timeToCheck)) {
                    wasCachedPeriodFound = true;
                    break;
                }
            }

            if (wasCachedPeriodFound) {
                //If we're building a missing interval, let's close it because we found and end
                if (isMissingIntervalBeingBuild) {
                    missingIntervals.add(new WeekInterval(missingStart, missingEnd));
                    isMissingIntervalBeingBuild = false;
                }
            } else {
                //We start building an interval of missing weeks
                if(isMissingIntervalBeingBuild){
                    missingEnd.addWeeks(1);
                } else {
                    isMissingIntervalBeingBuild = true;
                    missingStart = timeToCheck.copy();

                    missingEnd   = timeToCheck.copy();
                    missingEnd.addWeeks(+1);
                }
            }
        }

        //If we were building a missing interval, we must close it
        if (isMissingIntervalBeingBuild) {
            missingIntervals.add(new WeekInterval(missingStart, missingEnd));
        }
        return missingIntervals;
    }

    private static ArrayList<CachedLessonType> loadLessonTypes(boolean loadStudyCourses, boolean loadExtraCourses) {
        if (!loadStudyCourses && !loadExtraCourses) {
            throw new IllegalArgumentException("Cannot load nothing!");
        }

        String[] projection = {CLT_LESSON_TYPE_ID, CLT_NAME, CLT_COLOR, CLT_IS_EXTRA_COURSE};

        String where;
        String[] selectionArgs;

        if (loadStudyCourses && loadExtraCourses) {
            where = null;
            selectionArgs = null;
        } else if (loadStudyCourses) {
            where = CLT_IS_EXTRA_COURSE + " = ?";
            selectionArgs = new String[]{"0"};
        } else { //if(loadExtraCourses)
            where = CLT_IS_EXTRA_COURSE + " = ?";
            selectionArgs = new String[]{"1"};
        }

        Cursor cursor = writableDatabase.query(
                CACHED_LESSON_TYPES_TABLE, projection, where, selectionArgs, null, null, null
        );

        ArrayList<CachedLessonType> periods = new ArrayList<>();
        while (cursor.moveToNext()) {
            periods.add(CachedLessonType.fromLessonTypeCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    private static ArrayList<CachedLesson> loadCachedLessons(CachedPeriod cachedPeriod) {
        String[] projection = {
                CL_CACHED_PERIOD_ID,
                CL_LESSON_ID,
                CL_STARTS_AT_MS,
                CL_FINISHES_AT_MS,
                CL_TEACHING_ID,
                CL_WEEK_NUMBER,
                CL_YEAR,
                CL_SUBJECT,
                CL_ROOM,
                CL_DESCRIPTION,
                CL_COLOR
        };

        String selection;
        String[] selectionArgs;

        if (cachedPeriod.getLesson_type() == 0) {
            selection =  CL_CACHED_PERIOD_ID+" = ? ";
            selectionArgs = new String[]{
                    String.valueOf(cachedPeriod.getId())
            };
        } else { //it's an extra course: we load only it's lessons
            selection =  CL_CACHED_PERIOD_ID+" = ? AND "+ CL_TEACHING_ID+" = ?";
            selectionArgs = new String[]{
                String.valueOf(cachedPeriod.getId()),
                String.valueOf(cachedPeriod.getLesson_type()),
            };
        }

        Cursor cursor = writableDatabase.query(
            CACHED_LESSONS_TABLE, projection, selection, selectionArgs, null, null, null
        );

        ArrayList<CachedLesson> periods = new ArrayList<>();
        while(cursor.moveToNext()) {
            periods.add(CachedLesson.fromCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    /**
     * Deletes EVERYTHING about the current study course from the cache.
     */
    public static void purgeStudyCourseCache() {
        for (Long studyCourseCachedPeriodsId: getIdsOfCachedPeriodsOfStudyCourses()) {
            deleteStudyCourseLessons(studyCourseCachedPeriodsId);
            deleteCachedPeriodWithId(studyCourseCachedPeriodsId);
        }

        deleteAllLessonTypes(true);
    }

    private static ArrayList<Long> getIdsOfCachedPeriodsOfStudyCourses() {
        return queryForCachedPeriodIds(CP_LESSON_TYPE+" = 0");
    }

    private static ArrayList<Long> getIdsOfCachedPeriodsWithLessonType(int lessonTypeId) {
        return queryForCachedPeriodIds(CP_LESSON_TYPE+" = "+lessonTypeId);
    }

    @NonNull
    private static ArrayList<Long> queryForCachedPeriodIds(String where) {
        String[] projection = {CP_ID};
        Cursor query = writableDatabase.query(CACHED_PERIOD_TABLE, projection, where, null, null, null, null);

        ArrayList<Long> ids = new ArrayList<>();
        while(query.moveToNext()){
            ids.add(query.getLong(0));
        }

        query.close();

        return ids;
    }


    private static void deleteAllLessonTypes(boolean keepExtras) {
        String whereClause = null;
        if (keepExtras) {
            whereClause = CLT_IS_EXTRA_COURSE+" <> 1";
        }

        writableDatabase.delete(CACHED_LESSON_TYPES_TABLE, whereClause, null);
    }

    public static void removeExtraCoursesWithLessonType(int lessonTypeId) {
        for (Long cachedExtraId : getIdsOfCachedPeriodsWithLessonType(lessonTypeId)) {
            deleteLessonTypeWithId(lessonTypeId);
            deleteExtraLessonsOfType(cachedExtraId, lessonTypeId);
            deleteCachedPeriodWithId(cachedExtraId);
        }
    }

    /**
     * Deletes EVERYTHING from the case.
     */
    public static void obliterateCache() {
        writableDatabase.delete(CACHED_PERIOD_TABLE,       null, null);
        writableDatabase.delete(CACHED_LESSONS_TABLE,      null, null);
        writableDatabase.delete(CACHED_LESSON_TYPES_TABLE, null, null);
    }
}