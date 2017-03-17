package com.geridea.trentastico.network;

import com.android.volley.VolleyError;

import java.util.Calendar;

import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public interface LessonsFetchedListener {
    void onLoadingAboutToStart(CalendarInterval interval);

    void onLessonsLoaded(LessonsSet lessons, WeekInterval loadedInterval);

    void onErrorHappened(VolleyError error);

    void onParsingErrorHappened(Exception e);

    void onPartiallyCachedResultsFetched(CachedLessonsSet cacheSet);
}
