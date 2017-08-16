package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 28/04/2017.
 */

import com.geridea.trentastico.model.LibraryOpeningTimes

import java.util.Calendar

interface LibraryOpeningTimesListener {

    fun onOpeningTimesLoaded(times: LibraryOpeningTimes, date: Calendar)

    fun onOpeningTimesLoadingError()

    fun onErrorParsingResponse(e: Exception)
}
