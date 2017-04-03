package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.utils.time.WeekInterval;

public interface LessonsLoadingListener {
    void onLoadingAboutToStart(ILoadingMessage operation);
    void onLessonsLoaded(LessonsSet lessonsSet, WeekInterval interval, int operationId);
    void onErrorHappened(Exception error, int operationId);
    void onParsingErrorHappened(Exception exception, int operationId);
    void onLoadingDelegated(int operationId);
    void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet);
    void onNothingFoundInCache();

    /**
     * Called when the loading of the request has been aborted. Can happen when the request could
     * not be fetched and it doesn't allow retrials.
     */
    void onLoadingAborted(int operationId);


}
