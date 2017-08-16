package com.geridea.trentastico.network


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.model.cache.NotCachedInterval
import com.geridea.trentastico.utils.time.WeekInterval

import java.util.ArrayList

/**
 * Listener that dispatches a list of intervals of that are not cached (and probably will be loaded
 * from network).
 */
interface LoadingIntervalKnownListener {

    /**
     *
     * @param originalInterval the interval that we asked to load
     * @param intervals list of intervals that are not cached an have to be loaded
     */
    fun onIntervalsToLoadKnown(originalInterval: WeekInterval, intervals: ArrayList<NotCachedInterval>)

}
