package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class CutCachedPeriod {
    private CachedPeriod cachedPeriod;
    private ArrayList<WeekInterval> validIntervals = new ArrayList<>();

    public CutCachedPeriod(CachedPeriod cachedPeriod) {
        this.cachedPeriod = cachedPeriod;
    }

    public void addIntervalToKeep(WeekInterval interval) {
        validIntervals.add(interval);
    }

}
