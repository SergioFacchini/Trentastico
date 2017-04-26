package com.geridea.trentastico;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

public final class Config {

    //Debug
    public static final boolean DEBUG_MODE = BuildConfig.DEBUG;
    public static final int PRE_LOADING_WAITING_TIME_MS = 0;

    public static final boolean DEBUG_FORCE_ANOTHER_DATE = false;
    public static long DATE_TO_FORCE = 1491814432000L; //Mon Apr 10 10:55:32 2017 GMT

    //Networking
    /**
     * How much ms we should wait after a request has failed loading and the next retry?
     */
    public static final int WAITING_TIME_AFTER_A_REQUEST_FAILED = 5000;

    public static final boolean LAUNCH_REQUESTS_TO_DEBUG_SERVER = false;
    public static final String DEBUG_SERVER_URL = "http://ideagenesi.com/trentastico/lessons.json";

    //Database
    public static final String DATABASE_NAME = "data.db";
    public static final int DATABASE_VERSION = 2;

    //Calendar
    public static final int CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW = 3;

    //Lessons update service
    public static final int LESSONS_REFRESH_WAITING_HOURS = 4;
    public static final int LESSONS_REFRESH_POSTICIPATION_MINUTES = 5;

    public static final boolean QUICK_LESSON_CHECKS = false;
    public static final int DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS = 30;
    public static final int DEBUG_LESSONS_REFRESH_POSTICIPATION_SECONDS = 8;

    //Next lessons notification service
    public static final int NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN = 15;
}
