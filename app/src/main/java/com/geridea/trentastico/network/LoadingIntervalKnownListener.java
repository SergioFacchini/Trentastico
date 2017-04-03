package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public interface LoadingIntervalKnownListener {

    void onIntervalsToLoadKnown(WeekInterval originalInterval, ArrayList<NotCachedInterval> intervals);

}
