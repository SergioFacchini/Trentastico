package com.geridea.trentastico

import java.util.concurrent.TimeUnit


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

object Config {

    //Debug
    val PRE_LOADING_WAITING_TIME_MS = 0

    val DEBUG_FORCE_ANOTHER_DATE = false
    var DATE_TO_FORCE = 1491814432000L //Mon Apr 10 10:55:32 2017 GMT

    //Networking
    /**
     * How much ms we should wait after a request has failed loading and the next retry?
     */
    val WAITING_TIME_AFTER_A_REQUEST_FAILED = 5000

    val LAUNCH_LESSONS_REQUESTS_TO_DEBUG_SERVER = false

    val DEBUG_SERVER_URL = "https://ideagenesi.com/trentastico/lessons.json"

    //Database
    val DATABASE_NAME = "data.db"
    val DATABASE_VERSION = 3

    //Calendar
    const val CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW = 3

    //Lessons update service
    const val LESSONS_REFRESH_WAITING_REGULAR = 4L  //hours

    const val NEXT_LESSON_LOADING_WAIT_BETWEEN_ERRORS = 1  //hours

    /**
     * During diff of the cached times and the fresh ones, what is the last valid time to consider
     * for a notification? The times that are changed after the current_ms + anticipation_ms will
     * not be reported to the user (we don't want the user to be notified when a lesson scheduled
     * more than a month later changes).
     */
    val LESSONS_CHANGED_ANTICIPATION_MS = TimeUnit.MILLISECONDS.convert(14, TimeUnit.DAYS)

    //Billing
    const val BILLING_LICENCE = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnY6sQ6eYT3HS+OZyIg3/GrC9nEIiGytYZgDzOu/mI4BW9s50+tnopEWXdJw1fGhB4mBoH/JKjji8wj27D70Imj6T2qrRcdcJsTybwpRo1x/OpBVagu2dMssTkjQzQElnlV2PI4yNljJNvbuICXqFADSTIvL4510E4ozrs3Mq5nR4ZoththflWMEyTf5NwCyNZzGcxMRwu9wbPD7lZfgZMr9e9FXhNF4SSPs6F26M8nwxcZI2ZMJrWjezPcEW0Hd680Qc+7b5AjZpXipyF0r1yeIpCVww/JwIiRfXTdqwnCQVMr99qbcEoxUCXncWjy5VTU6uIhx3Pt560yUCjmYcIwIDAQAB"
    const val MERCHANT_ID = "11870918422535241858"

    //Next lessons notification service
    const val NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN = 15

    //Current study year
    const val CURRENT_STUDY_YEAR = "2024"

}
