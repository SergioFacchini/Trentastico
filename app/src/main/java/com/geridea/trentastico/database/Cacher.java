package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.Semester;
import com.geridea.trentastico.model.cache.CachedLesson;
import com.geridea.trentastico.model.cache.CachedLessonType;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.CachedPeriod;
import com.geridea.trentastico.network.LessonsRequest;
import com.geridea.trentastico.utils.StringUtils;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekIntervalCutResult;
import com.geridea.trentastico.utils.time.WeekTime;

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
            CP_LESSON_TYPE +  " INTEGER, " +
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
                CL_DESCRIPTION      + " VARCHAR(500) NOT NULL " +
        ")";


    //Cached lesson types
    public static final String CACHED_LESSON_TYPES_TABLE = "cached_lesson_types";

    public static final String CLT_LESSON_TYPE_ID = "lesson_type_id";
    public static final String CLT_NAME = "name";
    public static final String CLT_COLOR = "color";

    static final String SQL_CREATE_CACHED_LESSON_TYPES =
        "CREATE TABLE " + CACHED_LESSON_TYPES_TABLE + " (" +
            CLT_LESSON_TYPE_ID + " INTEGER NOT NULL, " +
            CLT_NAME +           " VARCHAR(500) NOT NULL, " +
            CLT_COLOR +          " INTEGER NOT NULL" +
        ")";


    private static SQLiteDatabase writableDatabase;

    public static void init(Context context){
        CacheDbHelper cacheDbHelper = new CacheDbHelper(context);
        writableDatabase = cacheDbHelper.getWritableDatabase();
    }

    public static void cacheLessonsSet(LessonsRequest request, LessonsSet setToCache) {
        //0) Overwrite existing lesson types
        //1) Clear the already existing lessons cached in the given interval
        //2) Add the new cache period
        //3) Get inserted id
        //4) Make lessons
        //5) Add lessons

        //Technically we should always be fetching the latest lesson types. In some cases, however
        //we can scroll back so much to be able to see the previous semesters' courses. We do not
        //want to cache courses that are not actual.
        for (LessonType lessonType : setToCache.getLessonTypes()) {
            LessonSchedule lesson = setToCache.getALessonHavingType(lessonType);
            if (lesson != null && Semester.isInCurrentSemester(lesson.getStartCal())) {
                deleteLessonTypeWithId(lessonType.getId());
                cacheLessonType(new CachedLessonType(lessonType));
            }
        }

        deleteCachedLessonsInInterval(request.getIntervalToLoad());

        long cachedPeriodId = insertCachedPeriod(new CachedPeriod(request.getIntervalToLoad()));
        for (LessonSchedule lesson : setToCache.getScheduledLessons()) {
            cacheLesson(new CachedLesson(cachedPeriodId, lesson));
        }
    }

    private static void cacheLessonType(CachedLessonType cachedLessonType) {
        ContentValues values = new ContentValues();
        values.put(CLT_LESSON_TYPE_ID, cachedLessonType.getLesson_type_id());
        values.put(CLT_NAME,           cachedLessonType.getName());
        values.put(CLT_COLOR,          cachedLessonType.getColor());

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

        writableDatabase.insert(CACHED_LESSONS_TABLE, null, values);
    }

    private static void deleteCachedLessonsInInterval(WeekInterval interval) {
        //Trimming existing cached periods so the passed interval won't have any overlapping
        //cached interval
        for (CachedPeriod period : loadCachePeriodsInInterval(interval, true)) {
            WeekIntervalCutResult cutResult = period.getPeriod().cutFromInterval(interval);

            if(!cutResult.hasAnyRemainingResult()){
                deleteCachedPeriodWithId(period.getId());
            } else if(cutResult.hasOnlyOneResult()){
                period.setPeriod(cutResult.getFirstRemaining());
                updateCachedPeriod(period);
            } else {
                splitCachePeriod(period, cutResult);
            }
        }

        deleteAllLessonsInInterval(interval);
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
        values.put(CP_START_WEEK,   period.getPeriod().getStart().getWeekNumber());
        values.put(CP_START_YEAR,   period.getPeriod().getStart().getYear());
        values.put(CP_END_WEEK,     period.getPeriod().getEnd()  .getWeekNumber());
        values.put(CP_END_YEAR,     period.getPeriod().getEnd()  .getYear());
        values.put(CP_LESSON_TYPE,  period.getLesson_type());
        values.put(CP_CACHED_IN_MS, period.getCached_in_ms());
        return values;
    }

    private static void deleteCachedPeriodWithId(long periodId) {
        String[] selectionArgs = { String.valueOf(periodId) };
        writableDatabase.delete(CACHED_PERIOD_TABLE, CP_ID+"= ?", selectionArgs);
    }

    private static void deleteAllLessonsInInterval(WeekInterval interval) {
        writableDatabase.delete(CACHED_LESSONS_TABLE, buildWeekTimeSelectionsSQL(interval), null);
    }

    private static ArrayList<CachedPeriod> loadCachePeriodsInInterval(WeekInterval intervalToLoad, boolean fetchOldCacheToo) {
        String[] projection = {
                CP_ID,
                CP_START_WEEK,
                CP_START_YEAR,
                CP_END_WEEK,
                CP_END_YEAR,
                CP_LESSON_TYPE,
                CP_CACHED_IN_MS
        };

        String selection = buildCachePeriodSelectionForInterval(intervalToLoad);
        if(!fetchOldCacheToo){
            selection = String.format(Locale.UK,
                    "(%s) AND (%s >= %d AND %s < %d)", selection, CP_CACHED_IN_MS, getLastValidCacheMs(),
                    CP_CACHED_IN_MS, System.currentTimeMillis()
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
        WeekInterval startIntervals = new WeekInterval(intervalToLoad.getStart(), intervalToLoad.getEnd());

        WeekTime endWeekInterval = intervalToLoad.getEnd().copy();
        endWeekInterval.addWeeks(-1);
        WeekInterval endIntervals   = new WeekInterval(intervalToLoad.getStart(), endWeekInterval);

        return buildWeekTimeSelectionsSQL(intervalToLoad, CP_START_WEEK, CP_START_YEAR) + " OR "+
               buildWeekTimeSelectionsSQL(intervalToLoad, CP_END_WEEK,   CP_END_YEAR);
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

    public static CachedLessonsSet getLessonsInFreshOrDeadCache(WeekInterval intervalToLoad) {
        CachedLessonsSet lessonsSet = new CachedLessonsSet();

        //Lesson types
        ArrayList<CachedLessonType> lessonTypes = loadLessonTypes();
        lessonsSet.addCachedLessonTypes(lessonTypes);

        //Cached periods
        ArrayList<CachedPeriod> cachePeriods = loadCachePeriodsInInterval(intervalToLoad, false);
        for (CachedPeriod cachedPeriod : cachePeriods) {
            lessonsSet.addLessonSchedules(loadLessonsOfCachePeriod(cachedPeriod, lessonTypes));
            lessonsSet.addCachedPeriod(cachedPeriod);
        }

        //Missing intervals
        lessonsSet.addMissingIntervals(findMissingIntervalsInCachePeriods(intervalToLoad, cachePeriods));

        return lessonsSet;
    }

    private static ArrayList<LessonSchedule> loadLessonsOfCachePeriod(CachedPeriod cachePeriod, ArrayList<CachedLessonType> lessonTypes) {
        ArrayList<LessonSchedule> lessons = new ArrayList<>();

        for (CachedLesson cachedLesson : loadCachedLessons(cachePeriod)) {
            CachedLessonType lessonType = getLessonTypeOfLesson(cachedLesson, lessonTypes);
            lessons.add(new LessonSchedule(cachedLesson, lessonType.getColor()));
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

    private static ArrayList<CachedLessonType> loadLessonTypes() {
        String[] projection = {CLT_LESSON_TYPE_ID, CLT_NAME, CLT_COLOR};

        Cursor cursor = writableDatabase.query(
                CACHED_LESSON_TYPES_TABLE, projection, null, null, null, null, null
        );

        ArrayList<CachedLessonType> periods = new ArrayList<>();
        while(cursor.moveToNext()) {
            periods.add(CachedLessonType.fromCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    /**
     * @return the first lesson having a given lesson type.
     */
    private static CachedLessonType getLessonTypeOfLesson(CachedLesson lesson, ArrayList<CachedLessonType> lessonTypes) {
        for (CachedLessonType lessonType : lessonTypes) {
            if (lessonType.getLesson_type_id() == lesson.getTeaching_id()) {
                return lessonType;
            }
        }

        //Technically, should never happen
        throw new RuntimeException("Could not find the requested lesson type");
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
                CL_DESCRIPTION
        };

        String selection = CL_CACHED_PERIOD_ID+" = ?";

        String[] selectionArgs = {
            String.valueOf(cachedPeriod.getId())
        };

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
}
