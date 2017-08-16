package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

import com.geridea.trentastico.utils.time.WeekInterval

import java.util.ArrayList

class CutCachedPeriod(private val cachedPeriod: CachedPeriod) {
    private val validIntervals = ArrayList<WeekInterval>()

    fun addIntervalToKeep(interval: WeekInterval) {
        validIntervals.add(interval)
    }

}
