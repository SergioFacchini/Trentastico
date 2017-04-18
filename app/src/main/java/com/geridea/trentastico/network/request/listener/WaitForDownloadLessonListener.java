package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.utils.time.WeekInterval;

public abstract class WaitForDownloadLessonListener implements LessonsLoadingListener{

    private int numRequestsSent, numRequestsSucceeded, numRequestsFailed;

    public WaitForDownloadLessonListener() {
        this.numRequestsSent = 0;
        this.numRequestsSucceeded = 0;
        this.numRequestsFailed = 0;
    }

    @Override
    public void onLoadingAboutToStart(ILoadingMessage operation) {
        numRequestsSent++;
    }

    @Override
    public void onLessonsLoaded(LessonsSet lessonsSet, WeekInterval interval, int operationId) {
        numRequestsSucceeded++;
        checkIfWeHaveFinished();
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
        //Should never happen since we do not manage loading from cache
        BugLogger.logBug("Cache used when shouldn't", new RuntimeException("Cache used when shouldn't"));
    }

    @Override
    public void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet) {
        //Should never happen since we do not manage loading from cache
        BugLogger.logBug("Cache used when shouldn't", new RuntimeException("Cache used when shouldn't"));
    }

    @Override
    public void onNothingFoundInCache() {
        //Nothing to do
    }

    @Override
    public void onLoadingAborted(int operationId) {
        numRequestsFailed++;
        checkIfWeHaveFinished();
    }

    public void onNothingToLoad(){
        onFinish(true);
    }

    private void checkIfWeHaveFinished() {
        if (numRequestsSent == (numRequestsSucceeded + numRequestsFailed)) {

            boolean withSuccess = numRequestsSent == numRequestsSucceeded;
            onFinish(withSuccess);
        }
    }

    public abstract void onFinish(boolean withSuccess);

}
