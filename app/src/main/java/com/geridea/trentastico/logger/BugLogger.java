package com.geridea.trentastico.logger;


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

import com.geridea.trentastico.model.ExtraCoursesList;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.utils.AppPreferences;

import org.acra.ACRA;

public class BugLogger {

    public static void logBug(String reason, Throwable e) {
        ACRA.getErrorReporter().putCustomData("REPORT-REASON", reason);
        ACRA.getErrorReporter().handleSilentException(e);
    }

    public static void setStudyCourse(StudyCourse course) {
        ACRA.getErrorReporter().putCustomData("STUDY-COURSE", course.toString());
    }

    public static void setExtraCourses(ExtraCoursesList extraCourses) {
        ACRA.getErrorReporter().putCustomData("EXTRA-COURSES", extraCourses.toJSON().toString());
    }

    public static void init() {
        setStudyCourse(AppPreferences.getStudyCourse());
        setExtraCourses(AppPreferences.getExtraCourses());
    }
}
