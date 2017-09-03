package com.geridea.trentastico

import java.util.concurrent.TimeUnit


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

object Config {

    //Debug
    val PRE_LOADING_WAITING_TIME_MS = 0

    var SHOW_DEBUG_MESSAGES = true

    val DEBUG_FORCE_ANOTHER_DATE = false
    var DATE_TO_FORCE = 1491814432000L //Mon Apr 10 10:55:32 2017 GMT

    //Networking
    /**
     * How much ms we should wait after a request has failed loading and the next retry?
     */
    val WAITING_TIME_AFTER_A_REQUEST_FAILED = 5000

    val LAUNCH_LESSONS_REQUESTS_TO_DEBUG_SERVER = false

    val DEBUG_SERVER_URL = "http://ideagenesi.com/trentastico/lessons.json"

    //Database
    val DATABASE_NAME = "data.db"
    val DATABASE_VERSION = 3

    //Calendar
    val CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW = 3

    //Lessons update service
    val LESSONS_REFRESH_WAITING_REGULAR = 4
    val LESSONS_REFRESH_WAITING_AFTER_ERROR = 2
    val LESSONS_REFRESH_POSTICIPATION_MINUTES = 5

    /**
     * During diff of the cached times and the fresh ones, what is the last valid time to consider
     * for a notification? The times that are changed after the current_ms + anticipation_ms will
     * not be reported to the user (we don't want the user to be notified when a lesson scheduled
     * more than a month later changes).
     */
    val LESSONS_CHANGED_ANTICIPATION_MS = TimeUnit.MILLISECONDS.convert(14, TimeUnit.DAYS)

    val QUICK_LESSON_CHECKS = false
    val DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS = 30
    val DEBUG_LESSONS_REFRESH_POSTICIPATION_SECONDS = 8

    //Next lessons notification service
    val NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN = 15

    //Current study year
    val CURRENT_STUDY_YEAR = "2017"

}
