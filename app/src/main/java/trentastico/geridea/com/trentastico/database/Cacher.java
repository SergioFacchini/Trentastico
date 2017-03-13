package trentastico.geridea.com.trentastico.database;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;

import trentastico.geridea.com.trentastico.model.LessonSchedule;
import trentastico.geridea.com.trentastico.model.LessonType;
import trentastico.geridea.com.trentastico.model.LessonsSet;
import trentastico.geridea.com.trentastico.model.SemesterUtils;
import trentastico.geridea.com.trentastico.model.StudyCourse;
import trentastico.geridea.com.trentastico.model.cache.CachedLesson;
import trentastico.geridea.com.trentastico.model.cache.CachedLessonType;
import trentastico.geridea.com.trentastico.model.cache.CachedLessonsSet;
import trentastico.geridea.com.trentastico.model.cache.CachedPeriod;
import trentastico.geridea.com.trentastico.network.LessonsRequest;

public class Cacher {

    private static SQLiteDatabase writableDatabase;

    public static void init(Context context){
        CacheDbHelper cacheDbHelper = new CacheDbHelper(context);
        writableDatabase = cacheDbHelper.getWritableDatabase();
    }

    public static void cacheLessonsSet(LessonsRequest request, LessonsSet setToCache) {
        //0) Clear the already existing lessons cached in the given interval
        //1) Add the new cache period
        //2) Get inserted id
        //3) Make lessons
        //4) Add lessons
        //5) Overwrite existing lesson types
        deleteCachedLessonsInInterval(
            request.getStudyCourse(), request.getFromWhenMs(), request.getToWhenMs()
        );

        long cachedPeriodId = insertCachedPeriod(new CachedPeriod(request));

        for (LessonSchedule lesson : setToCache.getScheduledLessons()) {
            cacheLesson(new CachedLesson(cachedPeriodId, lesson));
        }

        //Technically we should always be fetching the latest lesson types. In some cases, however
        //we can scroll back so much to be able to see the previous semesters' courses. We do not
        //want to cache courses that are not actual.
        for (LessonType lessonType : setToCache.getLessonTypes()) {
            LessonSchedule lesson = setToCache.getALessonHavingType(lessonType);
            if (lesson != null && SemesterUtils.isInCurrentSemester(lesson.getStartCal())) {
                deleteLessonTypeWithId(lessonType.getId());
                cacheLessonType(new CachedLessonType(lessonType));
            }
        }
    }

    private static void cacheLessonType(CachedLessonType cachedLessonType) {
        ContentValues values = new ContentValues();
        values.put("lesson_type_id", cachedLessonType.getLesson_type_id());
        values.put("name",           cachedLessonType.getName());
        values.put("color",          cachedLessonType.getColor());

        writableDatabase.insert("cached_lesson_types", null, values);
    }

    private static void deleteLessonTypeWithId(int lessonTypeId) {
        String selection = "lesson_type_id = ?";
        String[] selectionArgs = { String.valueOf(lessonTypeId) };

        writableDatabase.delete("cached_lesson_types", selection, selectionArgs);
    }

    private static void cacheLesson(CachedLesson cachedLesson) {
        ContentValues values = new ContentValues();
        values.put("cached_period_id", cachedLesson.getCached_period_id());
        values.put("lesson_id",        cachedLesson.getLesson_id());
        values.put("starts_at_ms",     cachedLesson.getStarts_at_ms());
        values.put("finishes_at_ms",   cachedLesson.getFinishes_at_ms());
        values.put("teaching_id",      cachedLesson.getTeaching_id());
        values.put("subject",          cachedLesson.getSubject());
        values.put("room",             cachedLesson.getRoom());
        values.put("description",      cachedLesson.getDescription());

        writableDatabase.insert("cached_lessons", null, values);
    }

    private static void deleteCachedLessonsInInterval(StudyCourse studyCourse, long from, long to) {
        ArrayList<CachedPeriod> periods = loadCachePeriodsInInterval(studyCourse, from, to);
        for (CachedPeriod period : periods) {
            period.cutFromPeriod(from, to);

            if(period.isEmpty()){
                deleteCachedPeriodWithId(period.getId());
            } else {
                updateCachedPeriod(period);
            }
        }

        deleteAllLessonsInInterval(from, to);
    }

    private static void updateCachedPeriod(CachedPeriod period) {
        ContentValues values = new ContentValues();
        values.put("id",            period.getId());
        values.put("department_id", period.getDepartment_id());
        values.put("course_id",     period.getCourse_id());
        values.put("year",          period.getYear());
        values.put("from_ms",       period.getFrom_ms());
        values.put("to_ms",         period.getTo_ms());
        values.put("lesson_type",   period.getLesson_type());
        values.put("cached_in_ms",  period.getCached_in_ms());

        // Which row to update, based on the title
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(period.getId()) };

        writableDatabase.update("cached_periods", values, selection, selectionArgs);
    }

    private static void deleteCachedPeriodWithId(long periodId) {
        String[] selectionArgs = { String.valueOf(periodId) };
        writableDatabase.delete("cached_periods", "id = ?", selectionArgs);
    }

    private static void deleteAllLessonsInInterval(long from, long to) {
        String selection = "starts_at_ms >= ? AND finishes_at_ms <= ? ";
        String[] selectionArgs = { String.valueOf(from), String.valueOf(to) };

        writableDatabase.delete("cached_lessons", selection, selectionArgs);
    }

    private static ArrayList<CachedPeriod> loadCachePeriodsInInterval(StudyCourse studyCourse, long from, long to) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                "id", "department_id", "course_id", "year", "from_ms", "to_ms", "lesson_type",
                "cached_in_ms"
        };

        // Filter results WHERE "title" = 'My Title'
        String selection =
                "department_id = ?" +
                " AND course_id = ?" +
                " AND year = ?" +
                " AND from_ms = ?" +
                " AND to_ms = ? ";

        String[] selectionArgs = {
                String.valueOf(studyCourse.getDepartmentId()),
                String.valueOf(studyCourse.getCourseId()),
                String.valueOf(studyCourse.getYear()),
                String.valueOf(from),
                String.valueOf(to)
        };


        Cursor cursor = writableDatabase.query(
            "cached_periods", projection, selection, selectionArgs, null, null, null
        );

        ArrayList<CachedPeriod> periods = new ArrayList<>();
        while(cursor.moveToNext()) {
            periods.add(CachedPeriod.fromCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    private static long insertCachedPeriod(CachedPeriod cachedPeriod) {
        ContentValues values = new ContentValues();
        values.put("department_id", cachedPeriod.getDepartment_id());
        values.put("course_id",     cachedPeriod.getCourse_id());
        values.put("year",          cachedPeriod.getYear());
        values.put("from_ms",       cachedPeriod.getFrom_ms());
        values.put("to_ms",         cachedPeriod.getTo_ms());
        values.put("lesson_type",   cachedPeriod.getLesson_type());
        values.put("cached_in_ms",  cachedPeriod.getCached_in_ms());

        long id = writableDatabase.insert("cached_periods", null, values);
        cachedPeriod.setId(id);

        return id;
    }

    public static CachedLessonsSet getLessonsInCacheIfAvailable(
            Calendar fromWhen, Calendar toWhen, StudyCourse studyCourse) {

        ArrayList<CachedPeriod> cachePeriods
                = loadCachePeriodsInInterval(studyCourse, fromWhen, toWhen);

        //TODO: do not use calendars, but WEEK_OF_YEAR AND YEAR associations
        for(Calendar week = (Calendar) fromWhen.clone();
            week.before(toWhen);
            week.add(Calendar.WEEK_OF_YEAR, +1)){

            if(!isWeekInPeriods(week, cachePeriods)){
                //There are some weeks not cached; unless we implement partial caching,
                //these won't be loaded
                return null;
            }
        }

        CachedLessonsSet lessonsSet = new CachedLessonsSet(studyCourse, fromWhen, toWhen, null);

        ArrayList<CachedLessonType> lessonTypes = loadLessonTypes();
        lessonsSet.addCachedLessonTypes(lessonTypes);
        lessonsSet.addLessonSchedules(loadLessons(studyCourse, fromWhen, toWhen, lessonTypes));

        return lessonsSet;
    }

    private static ArrayList<CachedLessonType> loadLessonTypes() {
        String[] projection = {"lesson_type_id", "name", "color"};

        Cursor cursor = writableDatabase.query(
                "cached_lesson_types", projection, null, null, null, null, null
        );

        ArrayList<CachedLessonType> periods = new ArrayList<>();
        while(cursor.moveToNext()) {
            periods.add(CachedLessonType.fromCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    private static ArrayList<LessonSchedule> loadLessons(StudyCourse studyCourse, Calendar fromWhen, Calendar toWhen, ArrayList<CachedLessonType> lessonTypes) {
        ArrayList<CachedLesson> cachedLessons = loadCachedLessons(studyCourse, fromWhen, toWhen);

        ArrayList<LessonSchedule> schedules = new ArrayList<>();
        for (CachedLesson cachedLesson : cachedLessons) {
            CachedLessonType lessonType = getLessonTypeOfLesson(cachedLesson, lessonTypes);
            schedules.add(new LessonSchedule(
                    cachedLesson.getLesson_id(),
                    cachedLesson.getRoom(),
                    cachedLesson.getSubject(),
                    cachedLesson.getStarts_at_ms(),
                    cachedLesson.getFinishes_at_ms(),
                    cachedLesson.getDescription(),
                    lessonType.getColor(),
                    cachedLesson.getTeaching_id()
            ));
        }

        return schedules;
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

    private static ArrayList<CachedLesson> loadCachedLessons(StudyCourse studyCourse, Calendar fromWhen, Calendar toWhen) {
        String[] projection = {
                "lesson_id", "cached_period_id", "room", "subject", "starts_at_ms",
                "finishes_at_ms", "description", "teaching_id"
        };

        String selection = "starts_at_ms >= ? AND finishes_at_ms <= ? ";

        String[] selectionArgs = {
                String.valueOf(fromWhen.getTimeInMillis()),
                String.valueOf(toWhen.getTimeInMillis())
        };

        Cursor cursor = writableDatabase.query(
                "cached_lessons", projection, selection, selectionArgs, null, null, null
        );

        ArrayList<CachedLesson> periods = new ArrayList<>();
        while(cursor.moveToNext()) {
            periods.add(CachedLesson.fromCursor(cursor));
        }
        cursor.close();

        return periods;
    }

    private static boolean isWeekInPeriods(Calendar fromWeek, ArrayList<CachedPeriod> periods) {
        Calendar toWeek = (Calendar) fromWeek.clone();
        toWeek.add(Calendar.WEEK_OF_YEAR, +1);

        for (CachedPeriod period : periods) {
            if(period.containsEntirely(fromWeek, toWeek)){
                return true;
            }
        }

        return false;
    }

    private static ArrayList<CachedPeriod> loadCachePeriodsInInterval(StudyCourse studyCourse, Calendar fromWhen, Calendar toWhen) {
        return loadCachePeriodsInInterval(studyCourse, fromWhen.getTimeInMillis(), toWhen.getTimeInMillis());
    }
}
