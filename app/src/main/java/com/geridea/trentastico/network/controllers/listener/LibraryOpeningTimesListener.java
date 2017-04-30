package com.geridea.trentastico.network.controllers.listener;


/*
 * Created with ♥ by Slava on 28/04/2017.
 */

import com.geridea.trentastico.model.LibraryOpeningTimes;

import java.util.Calendar;

public interface LibraryOpeningTimesListener {

    void onOpeningTimesLoaded(LibraryOpeningTimes times, Calendar date);

    void onOpeningTimesLoadingError();

    void onErrorParsingResponse(Exception e);
}
