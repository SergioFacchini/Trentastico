package com.geridea.trentastico.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.PartitioningCase;
import com.geridea.trentastico.model.StudyCourse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AppPreferences {

    private static Context appContext;

    public static void init(Context context) {
        appContext = context;
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
        SharedPreferences.Editor editor = get().edit();
        editor.putBoolean("IS_FIRST_RUN", isFirstRun);
        editor.apply();
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

        SharedPreferences.Editor editor = get().edit();
        editor.putString("FILTERED_TEACHINGS", array.toString());
        editor.apply();
    }

    public static boolean hasLessonTypeWithIdHidden(int id) {
        return getLessonTypesIdsToHide().contains(id);
    }

    public static void removeAllHiddenCourses() {
        setLessonTypesIdsToHide(new ArrayList<Integer>());
    }

    public static void setCalendarNumOfDaysToShow(int numOfDays) {
        SharedPreferences.Editor editor = get().edit();
        editor.putInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", numOfDays);
        editor.apply();
    }

    public static int getCalendarNumOfDaysToShow() {
        return get().getInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", Config.CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW);
    }

    public static void setPartitioningsToHide(int lessonTypeId, ArrayList<PartitioningCase> partitioningCases) {
        try {
            //Building values array
            JSONArray jsonArrayCases = new JSONArray();
            for (PartitioningCase aCase : partitioningCases) {
                jsonArrayCases.put(aCase.getCase());
            }

            //Saving partitionings
            JSONObject partitioningJSON = getPartitioningsJSON();
            partitioningJSON.put(String.valueOf(lessonTypeId), jsonArrayCases);

            SharedPreferences.Editor editor = get().edit();
            editor.putString("PARTITIONINGS_TO_HIDE", partitioningJSON.toString());
            editor.apply();
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
        SharedPreferences.Editor editor = get().edit();
        editor.putString("PARTITIONINGS_TO_HIDE", "{}");
        editor.apply();
    }
}