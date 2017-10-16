package com.geridea.trentastico.utils

import com.geridea.trentastico.BuildConfig
import com.geridea.trentastico.network.Networker


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

        //NOTE: version 10 does not exists because of a google problem

        if(lastVersion in 6..7) { //0.9.5 - 0.9.6
            //I've fixed that "timestamp" inconsistency bug.
            AppPreferences.debugSkipNextLessonChangedNotification = true
        }

        if(lastVersion in 6..9) { //0.9.5 - 0.9.8
            //Last fixes for the problem about the service that crashes while updating lesson
            //(see #148)
            Networker.obliterateCache()
        }

        AppPreferences.lastVersionExecuted = thisVersion
    }

}