package com.geridea.trentastico.utils

import android.content.Context
import com.evernote.android.job.JobManager
import com.geridea.trentastico.BuildConfig
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.LessonsUpdaterJob
import com.geridea.trentastico.services.NextLessonNotificationShowService


/*
 * Created with ♥ by Slava on 15/09/2017.
 */

/**
 * Perform maintenance operations when the version of the app changes.
 */
object VersionManager {

    fun checkForVersionChangeCode(context: Context) {
        val lastVersion = AppPreferences.lastVersionExecuted
        val thisVersion = BuildConfig.VERSION_CODE

        if (lastVersion == thisVersion) {
            return
        }

        if(lastVersion in 14..16) {
            //Fixing the bug "Apps may not schedule more than 100 distinct jobs" in the
            //new scheduling system
            JobManager.instance().cancelAllForTag(NextLessonNotificationShowService.TAG)
        }

        if(lastVersion in 1..14) {
            if(!AppPreferences.nextLessonNotificationsEnabled) {
                NextLessonNotificationShowService.clearNotifications(context)
                NextLessonNotificationShowService.cancelScheduling()
            }
        }

        if(lastVersion in 1..13) {
            //Some people has courses name "Interazione Uomo" without "- Macchina"
            Networker.obliterateCache()

            //Removed some preferences that won't be used anymore
            AppPreferences._removePreferenceByName("NEXT_LESSONS_UPDATE_TIME")
            AppPreferences._removePreferenceByName("WAS_LAST_TIMES_CHECK_SUCCESSFUL")
            AppPreferences._removePreferenceByName("SKIP_NEXT_LESSON_CHANGED_NOTIFICATION")
            AppPreferences._removePreferenceByName("APP_IS_IN_BETA_MESSAGE_SHOWN")

            //Introduced the new scheduling system
            LessonsUpdaterJob.schedulePeriodicRun()

            AppPreferences.notificationTracker.clear()
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

        if(lastVersion in 1..19){
            //The academic year has changed: 2019 - 2020
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()

            AppPreferences.hasToUpdateStudyCourse = true
        }

        if(lastVersion in 1..21){
            //The academic year has changed: 2020 - 2021
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()

            AppPreferences.hasToUpdateStudyCourse = true
        }

        if(lastVersion in 1..25){
            //The academic year has changed: 2021 - 2022
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()

            AppPreferences.hasToUpdateStudyCourse = true
        }

        //Update of EasyAccademy
        if(lastVersion in 1..30){
            Networker.obliterateCache()
        }


        if (lastVersion in 1..31) {
            //The academic year has changed: 2022 - 2023
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()

            AppPreferences.hasToUpdateStudyCourse = true
        }

        if (lastVersion in 1..34) {
            //The academic year has changed: 2023 - 2024
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()

            AppPreferences.hasToUpdateStudyCourse = true
        }

        if (lastVersion in 1..35) {
            //The academic year has changed: 2024 - 2025
            Networker.obliterateCache()
            AppPreferences.clearLessonsToHide()
            AppPreferences.removeAllExtraCourses()
            AppPreferences.removeStudyCourse()

            AppPreferences.hasToUpdateStudyCourse = true
        }

        AppPreferences.lastVersionExecuted = thisVersion
    }

}
