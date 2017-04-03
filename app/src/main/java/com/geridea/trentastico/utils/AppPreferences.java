package com.geridea.trentastico.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.ExtraCoursesList;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.PartitioningCase;
import com.geridea.trentastico.model.StudyCourse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AppPreferences {

    private static Context appContext;

    /**
     * Here we're caching the extra courses list. It's being deserialized very often and this
     * caching is made to prevent this.
     */
    private static ExtraCoursesList extraCourses = new ExtraCoursesList();

    public static void init(Context context) {
        appContext = context;

        extraCourses = readExtraCourses();
    }

    private static SharedPreferences get() {
        if(appContext == null) {
            throw new RuntimeException(
                    "Preferences should be initialized by calling Preferences.init(...) method");
        } else {
            return appContext.getSharedPreferences("null", Context.MODE_PRIVATE);
        }
    }

    public static void setIsFirstRun(boolean isFirstRun) {
        putBoolean("IS_FIRST_RUN", isFirstRun);
    }

    /**
     * @return true if tis the first time the application is run
     */
    public static boolean isFirstRun() {
        return get().getBoolean("IS_FIRST_RUN", true);
    }

    public static void setStudyCourse(StudyCourse course) {
        SharedPreferences.Editor editor = get().edit();
        editor.putLong("STUDY_DEPARTMENT", course.getDepartmentId());
        editor.putLong("STUDY_COURSE",     course.getCourseId());
        editor.putInt("STUDY_YEAR",        course.getYear());
        editor.apply();
    }

    public static StudyCourse getStudyCourse() {
        return new StudyCourse(
           get().getLong("STUDY_DEPARTMENT", 0),
           get().getLong("STUDY_COURSE",     0),
           get().getInt("STUDY_YEAR",        0)
        );
    }

    public static ArrayList<Integer> getLessonTypesIdsToHide(){
        ArrayList<Integer> lessonTypesIds = new ArrayList<>();

        String filteredJSON = get().getString("FILTERED_TEACHINGS", "[]");

        try {
            JSONArray json = new JSONArray(filteredJSON);
            for(int i = 0; i<json.length(); i++){
                lessonTypesIds.add(json.getInt(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return lessonTypesIds;
    }

    public static void setLessonTypesIdsToHide(ArrayList<Integer> teachings) {
        JSONArray array = new JSONArray();
        for (Integer teachingId : teachings) {
            array.put(teachingId);
        }

        putString("FILTERED_TEACHINGS", array.toString());
    }

    public static boolean hasLessonTypeWithIdHidden(int id) {
        return getLessonTypesIdsToHide().contains(id);
    }

    public static void removeAllHiddenCourses() {
        setLessonTypesIdsToHide(new ArrayList<Integer>());
    }

    public static void setCalendarNumOfDaysToShow(int numOfDays) {
        putInt(numOfDays, "CALENDAR_NUM_OF_DAYS_TO_SHOW");
    }

    private static void putInt(int numOfDays, String key) {
        SharedPreferences.Editor editor = get().edit();
        editor.putInt(key, numOfDays);
        editor.apply();
    }

    public static int getCalendarNumOfDaysToShow() {
        return get().getInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", Config.CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW);
    }

    private static void setPartitioningsToHide(int lessonTypeId, ArrayList<PartitioningCase> partitioningCases) {
        try {
            //Building values array
            JSONArray jsonArrayCases = new JSONArray();
            for (PartitioningCase aCase : partitioningCases) {
                jsonArrayCases.put(aCase.getCase());
            }

            //Saving partitionings
            JSONObject partitioningJSON = getPartitioningsJSON();
            partitioningJSON.put(String.valueOf(lessonTypeId), jsonArrayCases);

            putString("PARTITIONINGS_TO_HIDE", partitioningJSON.toString());
        } catch (JSONException e) {
            BugLogger.logBug();
            e.printStackTrace();
        }
    }

    private static JSONObject getPartitioningsJSON() {
        try {
            return new JSONObject(get().getString("PARTITIONINGS_TO_HIDE", "{}"));
        } catch (JSONException e) {
            BugLogger.logBug();
            e.printStackTrace();

            throw new RuntimeException("Error reading partitionings JSON.");
        }
    }

    public static ArrayList<String> getHiddenPartitionings(int id) {
        ArrayList<String> partitionings = new ArrayList<>();

        try {
            JSONArray hiddenPartitioningsArray = getPartitioningsJSON().optJSONArray(String.valueOf(id));
            if (hiddenPartitioningsArray != null) {
                for (int i = 0; i < hiddenPartitioningsArray.length(); i++) {
                    partitionings.add(hiddenPartitioningsArray.getString(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return partitionings;
    }

    public static void updatePartitioningsToHide(LessonType lesson) {
        setPartitioningsToHide(lesson.getId(), lesson.findPartitioningsToHide());
    }

    public static void removeAllHiddenPartitionings() {
        putString("PARTITIONINGS_TO_HIDE", "{}");
    }

    public static ExtraCoursesList getExtraCourses() {
        return extraCourses;
    }

    @NonNull
    private static ExtraCoursesList readExtraCourses() {
        ExtraCoursesList courses = new ExtraCoursesList();

        try {
            JSONArray jsonArray = new JSONArray(get().getString("EXTRA_COURSES", "[]"));
            for (int i = 0; i < jsonArray.length(); i++) {
                courses.add(ExtraCourse.fromJSON(jsonArray.getJSONObject(i)));
            }
            return courses;
        } catch (JSONException e) {
            BugLogger.logBug();
            e.printStackTrace();

            throw new RuntimeException("Could not get extra courses from AppPreferences.");
        }
    }

    public static void addExtraCourse(ExtraCourse course) {
        extraCourses.add(course);

        saveExtraCourses();
    }

    private static void saveExtraCourses() {
        putString("EXTRA_COURSES", extraCourses.toJSON().toString());
    }

    private static void putString(String key, String value) {
        SharedPreferences.Editor editor = get().edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static boolean hasExtraCourseWithId(int lessonTypeId) {
        return extraCourses.hasCourseWithId(lessonTypeId);
    }

    public static void removeExtraCoursesHaving(long courseId, int year) {
        extraCourses.removeHaving(courseId, year);
        saveExtraCourses();
    }

    public static ArrayList<ExtraCourse> getExtraCoursesHaving(long courseId, int year) {
        return extraCourses.getExtraCoursesHaving(courseId, year);
    }

    public static void removeExtraCourse(int lessonTypeId) {
        extraCourses.removeHavingLessonType(lessonTypeId);
        saveExtraCourses();
    }

    public static boolean isStudyCourseSet() {
        return get().getLong("STUDY_DEPARTMENT", 0) != 0 &&
               get().getLong("STUDY_COURSE",     0) != 0 &&
               get().getInt( "STUDY_YEAR",       0) != 0;
    }

    public static void setNextLessonsUpdateTime(long time) {
        putLong("NEXT_LESSONS_UPDATE_TIME", time);
    }

    private static void putLong(String key, long time) {
        SharedPreferences.Editor editor = get().edit();
        editor.putLong(key, time);
        editor.apply();
    }

    public static long getNextLessonsUpdateTime() {
        return get().getLong("NEXT_LESSONS_UPDATE_TIME", 0);
    }

    public static boolean hadInternetInLastCheck() {
        return get().getBoolean("HAD_INTERNET_DURING_LAST_LESSON_UPDATE", true);
    }

    public static void hadInternetInLastCheck(boolean had) {
        putBoolean("HAD_INTERNET_DURING_LAST_LESSON_UPDATE", had);
    }

    private static void putBoolean(String key, boolean bool) {
        SharedPreferences.Editor editor = get().edit();
        editor.putBoolean(key, bool);
        editor.apply();
    }

    public static boolean isSearchForLessonChangesEnabled() {
        return get().getBoolean("SEARCH_LESSON_CHANGES", true);
    }

    public static void setSearchForLessonChangesEnabled(boolean enabled) {
        putBoolean("SEARCH_LESSON_CHANGES", enabled);
    }

    public static boolean isNotificationForLessonChangesEnabled() {
        return get().getBoolean("SHOW_NOTIFICATION_ON_LESSON_CHANGES", true);
    }

    public static void setNotificationForLessonChangesEnabled(boolean enabled) {
        putBoolean("SHOW_NOTIFICATION_ON_LESSON_CHANGES", enabled);
    }
}