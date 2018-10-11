package com.geridea.trentastico.utils

import com.geridea.trentastico.BuildConfig
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.LessonsUpdaterJob


/*
 * Created with ♥ by Slava on 15/09/2017.
 */

/**
 * Perform maintenance operations when the version of the app changes.
 */
object VersionManager {

    fun checkForVersionChangeCode() {
        val lastVersion = AppPreferences.lastVersionExecuted
        val thisVersion = BuildConfig.VERSION_CODE

        if (lastVersion == thisVersion) {
            return
        }

        if(lastVersion in 1..13) {
            //Some people has courses name "Interazione Uomo" without "- Macchina"
            Networker.obliterateCache()

            //Removed some preferences that won't be used anymore
            AppPreferences._removePreferenceByName("NEXT_LESSONS_UPDATE_TIME")
            AppPreferences._removePreferenceByName("WAS_LAST_TIMES_CHECK_SUCCESSFUL")
            AppPreferences._removePreferenceByName("SKIP_NEXT_LESSON_CHANGED_NOTIFICATION")

            //Introduced the new scheduling system
            LessonsUpdaterJob.schedulePeriodicRun()
        }

        if(lastVersion in 1..12) { //> 1.0.1
            //The academic year has changed and the user must choose another year
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()
            AppPreferences.hasToUpdateStudyCourse = true
        }

        if(lastVersion in 6..9) { //0.9.5 - 0.9.8
            //Last fixes for the problem about the service that crashes while updating lesson
            //(see #148)
            Networker.obliterateCache()
        }

        if(lastVersion in 1..11){
            //There are no more 7-day views, these are now 5-days views. #152
            if(AppPreferences.calendarNumOfDaysToShow == 7){
                AppPreferences.calendarNumOfDaysToShow = 5
            }
        }

        AppPreferences.lastVersionExecuted = thisVersion
    }

}