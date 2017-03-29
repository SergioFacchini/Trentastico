package com.geridea.trentastico;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

public final class Config {

    //Debug
    public static final boolean DEBUG_MODE = false;
    public static final int PRE_LOADING_WAITING_TIME_MS = 0;

    //Networking
    /**
     * How much ms we should wait after a request has failed loading and the next retry?
     */
    public static final int WAITING_TIME_AFTER_A_REQUEST_FAILED = 5000;

    //Database
    public static final String DATABASE_NAME = "data.db";
    public static final int DATABASE_VERSION = 1;

    //Calendar
    public static final int CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW = 2;

    //Services
    public static final int LESSONS_REFRESH_RATE_HOURS = 3;
    public static final int LESSONS_REFRESH_WAITING_HOURS = 4;

    public static final int DEBUG_LESSONS_REFRESH_RATE_SECONDS = 30;
    public static final int DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS = 60;
}
