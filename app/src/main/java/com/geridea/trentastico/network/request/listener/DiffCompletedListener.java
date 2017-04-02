package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.utils.time.WeekInterval;

public class DiffCompletedListener implements LessonsLoadingListener {

    private LessonsDifferenceListener differenceListener;

    public DiffCompletedListener(LessonsDifferenceListener differenceListener) {
        this.differenceListener = differenceListener;
    }

    @Override
    public void onLoadingAboutToStart(ILoadingMessage operation) {
        //Nothing to do
    }

    @Override
    public void onLessonsLoaded(LessonsSet lessonsSet, WeekInterval interval, int operationId) {
        differenceListener.onRequestCompleted();
    }

    @Override
    public void onErrorHappened(Exception error, int operationId) {
        //Managed in onLoadingAborted
    }

    @Override
    public void onParsingErrorHappened(Exception exception, int operationId) {
        //Managed in onLoadingAborted
    }

    @Override
    public void onLoadingDelegated(int operationId) {
        //We disabled the cache, this should never happen
        BugLogger.logBug();
    }

    @Override
    public void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet) {
        //We disabled the cache, this should never happen
        BugLogger.logBug();
    }

    @Override
    public void onLoadingAborted(int operationId) {
        differenceListener.onLoadingError();
    }
}
