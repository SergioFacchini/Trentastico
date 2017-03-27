package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.android.volley.VolleyError;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.operations.ILoadingOperation;
import com.geridea.trentastico.utils.time.WeekInterval;

public interface LessonsLoadingListener {
    void onLoadingAboutToStart(ILoadingOperation operation);
    void onLessonsLoaded(LessonsSet lessonsSet, WeekInterval interval);
    void onErrorHappened(VolleyError error);
    void onParsingErrorHappened(Exception exception);
    void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet);
}
