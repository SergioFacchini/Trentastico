package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.android.volley.VolleyError;
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.utils.time.WeekInterval;

public interface LessonsLoadingListener {
    void onLoadingAboutToStart(ILoadingMessage operation);
    void onLessonsLoaded(LessonsSet lessonsSet, WeekInterval interval, int operationId);
    void onErrorHappened(VolleyError error, int operationId);
    void onParsingErrorHappened(Exception exception, int operationId);
    void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet);
}
