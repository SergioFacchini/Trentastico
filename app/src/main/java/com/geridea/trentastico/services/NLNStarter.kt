package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import java.io.Serializable

/**
 * Next Lesson Notification Starter
 */
enum class NLNStarter : Serializable {
    DEBUG,
    PHONE_BOOT,
    NETWORK_ON,
    APP_BOOT,
    STUDY_COURSE_CHANGE,
    NOTIFICATIONS_SWITCHED_ON,
    FILTERS_CHANGED,
    EXTRA_COURSE_CHANGE,
    ALARM_MORNING,
    ALARM_LESSON
}
