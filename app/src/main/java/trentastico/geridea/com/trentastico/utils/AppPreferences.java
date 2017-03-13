package trentastico.geridea.com.trentastico.utils;

import android.content.Context;
import android.content.SharedPreferences;

import trentastico.geridea.com.trentastico.model.StudyCourse;

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
        editor.putLong("STUDY_DEPARTMENT", course.getDepartment());
        editor.putLong("STUDY_COURSE",     course.getCourse());
        editor.putLong("STUDY_YEAR",       course.getYear());
        editor.apply();
    }

    public static StudyCourse getStudyCourse() {
        return new StudyCourse(
           get().getLong("STUDY_DEPARTMENT", 0),
           get().getLong("STUDY_COURSE",     0),
           get().getLong("STUDY_YEAR",       0)
        );
    }

}