package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.model.cache.NotCachedInterval;

import java.util.ArrayList;

public interface NotCachedIntervalsListener {

    void onIntervalsKnown(ArrayList<NotCachedInterval> notCachedIntervals);

}
