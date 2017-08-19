package com.geridea.trentastico


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

object Config {

    //Debug
    val DEBUG_MODE = BuildConfig.DEBUG
    val PRE_LOADING_WAITING_TIME_MS = 0

    var SHOW_DEBUG_MESSAGES = true

    val DEBUG_FORCE_ANOTHER_DATE = false
    var DATE_TO_FORCE = 1491814432000L //Mon Apr 10 10:55:32 2017 GMT

    //Networking
    /**
     * How much ms we should wait after a request has failed loading and the next retry?
     */
    val WAITING_TIME_AFTER_A_REQUEST_FAILED = 5000

    val LAUNCH_REQUESTS_TO_DEBUG_SERVER = false
    val DEBUG_SERVER_URL = "http://ideagenesi.com/trentastico/lessons.json"

    //Database
    val DATABASE_NAME = "data.db"
    val DATABASE_VERSION = 3

    //Calendar
    val CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW = 3

    //Lessons update service
    val LESSONS_REFRESH_WAITING_HOURS = 4
    val LESSONS_REFRESH_POSTICIPATION_MINUTES = 5

    val QUICK_LESSON_CHECKS = false
    val DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS = 30
    val DEBUG_LESSONS_REFRESH_POSTICIPATION_SECONDS = 8

    //Next lessons notification service
    val NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN = 15

    //Current study year
    val CURRENT_STUDY_YEAR = "2017"

}
