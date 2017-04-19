package com.geridea.trentastico.logger;


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import android.support.annotation.NonNull;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.model.ExtraCoursesList;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.utils.AppPreferences;

import org.acra.ACRA;
import org.acra.ErrorReporter;

public class BugLogger {

    public static void logBug(String reason, Throwable e) {
        if(Config.DEBUG_MODE){
            return;
        }

        getErrorReporter().putCustomData("REPORT-REASON", reason);
        getErrorReporter().handleSilentException(e);
    }

    public static void setStudyCourse(StudyCourse course) {
        if(Config.DEBUG_MODE){
            return;
        }

        getErrorReporter().putCustomData("STUDY-COURSE", course.toString());
    }

    public static void setExtraCourses(ExtraCoursesList extraCourses) {
        if(Config.DEBUG_MODE){
            return;
        }

        getErrorReporter().putCustomData("EXTRA-COURSES", extraCourses.toJSON().toString());
    }

    @NonNull
    private static ErrorReporter getErrorReporter() {
        return ACRA.getErrorReporter();
    }

    public static void init() {
        if(Config.DEBUG_MODE){
            return;
        }

        getErrorReporter().putCustomData("android-id", AppPreferences.getAndroidId());

        setStudyCourse(AppPreferences.getStudyCourse());
        setExtraCourses(AppPreferences.getExtraCourses());
    }
}
