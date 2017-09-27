package com.geridea.trentastico.utils

import com.geridea.trentastico.BuildConfig


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

        when (lastVersion) {
            in 6..7 -> { //0.9.5 - 0.9.6
                //I've fixed that "timestamp" inconsistency bug.
                AppPreferences.debugSkipNextLessonChangedNotification = true
            }
        }

        AppPreferences.lastVersionExecuted = thisVersion
    }

}