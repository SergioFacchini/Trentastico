package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.model.cache.NotCachedInterval

import java.util.ArrayList

interface NotCachedIntervalsListener {

    fun onIntervalsKnown(notCachedIntervals: ArrayList<NotCachedInterval>)

}
