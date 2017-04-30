package com.geridea.trentastico.network.controllers.listener;


/*
 * Created with ♥ by Slava on 29/04/2017.
 */

import com.geridea.trentastico.model.LibraryOpeningTimes;

/**
 * Listener that returns from the cache the opening times of the libraries
 */
public interface CachedLibraryOpeningTimesListener {

    void onCachedOpeningTimesFound(LibraryOpeningTimes times);

    void onNoCachedOpeningTimes();

}
