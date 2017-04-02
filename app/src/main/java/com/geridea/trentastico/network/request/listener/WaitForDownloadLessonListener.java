package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.utils.listeners.GenericListener1;
import com.geridea.trentastico.utils.time.WeekInterval;

public class WaitForDownloadLessonListener implements LessonsLoadingListener {

    private int numRequestsSent, numRequestsSucceeded, numRequestsFailed;

    private final GenericListener1<Boolean> listenerToNotify;

    public WaitForDownloadLessonListener(GenericListener1<Boolean> listenerToNotify) {
        this.listenerToNotify = listenerToNotify;

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
        BugLogger.logBug();
    }

    @Override
    public void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet) {
        //Should never happen since we do not manage loading from cache
        BugLogger.logBug();
    }

    @Override
    public void onLoadingAborted(int operationId) {
        numRequestsFailed++;
        checkIfWeHaveFinished();
    }

    public void onNothingToLoad(){
        listenerToNotify.onFinish(true);
    }

    private void checkIfWeHaveFinished() {
        if (numRequestsSent == (numRequestsSucceeded + numRequestsFailed)) {

            boolean withSuccess = numRequestsSent == numRequestsSucceeded;
            listenerToNotify.onFinish(withSuccess);
        }
    }
}
