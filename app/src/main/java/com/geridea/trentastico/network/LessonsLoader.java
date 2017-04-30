package com.geridea.trentastico.network;

import com.geridea.trentastico.gui.views.ScrollDirection;
import com.geridea.trentastico.gui.views.requestloader.FinishedLoadingFromCacheMessage;
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.gui.views.requestloader.LoadingFromCacheMessage;
import com.geridea.trentastico.gui.views.requestloader.NetworkErrorMessage;
import com.geridea.trentastico.gui.views.requestloader.NoOperationMessage;
import com.geridea.trentastico.gui.views.requestloader.ParsingErrorMessage;
import com.geridea.trentastico.gui.views.requestloader.TerminalMessage;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal3;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static com.geridea.trentastico.gui.views.ScrollDirection.LEFT;


public class LessonsLoader implements LessonsLoadingListener, LoadingIntervalKnownListener {

    /**
     * Dispatched when the loading of events has been completed and the calendar can be made
     * visible.
     */
    public final Signal3<LessonsSet, WeekInterval, ILoadingMessage> onLoadingOperationSuccessful = new Signal3<>();

    /**
     * Dispatched when some results were retrieved from the cache, however some results are still
     * missing and will have to be loaded from network.
     */
    public final Signal1<CachedLessonsSet> onPartiallyCachedResultsFetched = new Signal1<>();

    /**
     * Dispatched when the calendar starts loading something from internet. The argument is that
     * "something".
     */
    public final Signal1<ILoadingMessage> onLoadingMessageDispatched = new Signal1<>();

    private final List<WeekInterval> loadingIntervals = Collections.synchronizedList(new ArrayList<WeekInterval>());

    public LessonsLoader() {

    }

    private void loadAndAddLessons(final WeekInterval intervalToLoad) {
        addLoadingInterval(intervalToLoad);
        Networker.loadLessons(intervalToLoad, this, this);

        onLoadingMessageDispatched.dispatch(new LoadingFromCacheMessage(intervalToLoad));
    }

    @Override
    public void onIntervalsToLoadKnown(WeekInterval originalInterval, ArrayList<NotCachedInterval> intervals) {
        addLoadingIntervals(intervals);
        removeLoadingInterval(originalInterval);
    }

    private void addLoadingIntervals(ArrayList<NotCachedInterval> intervalsToLoad) {
        for (WeekInterval interval : intervalsToLoad) {
            addLoadingInterval(interval);
        }
    }

    private void addLoadingInterval(WeekInterval intervalToLoad) {
        synchronized (loadingIntervals) {
            loadingIntervals.add(intervalToLoad);
        }
    }

    private void removeLoadingInterval(WeekInterval intervalToRemove) {
        synchronized (loadingIntervals) {
            loadingIntervals.remove(intervalToRemove);
        }
    }

    private boolean isWeekAlreadyBeingLoaded(WeekTime week) {
        synchronized (loadingIntervals) {
            for (WeekInterval loadingInterval : loadingIntervals) {
                if (loadingInterval.contains(week)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void loadDaysIfNeeded(WeekTime weekToLoad, ScrollDirection direction) {
        if (!isWeekAlreadyBeingLoaded(weekToLoad)) {
            WeekTime loadFrom, loadTo;
            if(direction == LEFT){
                loadFrom = weekToLoad.copy();
                loadFrom.addWeeks(-2);

                loadTo = weekToLoad.copy();
                loadTo.addWeeks(+1);
            } else {
                //We scrolled forward
                loadFrom = weekToLoad.copy();

                loadTo = loadFrom.copy();
                loadTo.addWeeks(+2);
            }

            loadAndAddLessons(new WeekInterval(loadFrom, loadTo));
        }
    }

    public void loadEventsNearDay(Calendar day) {
        WeekTime fromWT = new WeekTime(day);
        fromWT.addWeeks(-1);

        WeekTime toWT = new WeekTime(day);
        toWT.addWeeks(+2);

        loadAndAddLessons(new WeekInterval(fromWT, toWT));
    }

    @Override
    public void onLoadingAboutToStart(ILoadingMessage loadingOperation) {
        onLoadingMessageDispatched.dispatch(loadingOperation);
    }

    @Override
    public void onLessonsLoaded(LessonsSet lessons, WeekInterval interval, int operationId) {
        //We might have gotten this lessons from cache:
        onLoadingMessageDispatched.dispatch(new FinishedLoadingFromCacheMessage());

        removeLoadingInterval(interval);

        //Operation id is null when we loaded the lesson without starting any new request
        //(example: fetched from cache)
        ILoadingMessage message = (operationId == 0) ? new NoOperationMessage() : new TerminalMessage(operationId);

        onLoadingOperationSuccessful.dispatch(lessons, interval, message);
    }

    @Override
    public void onErrorHappened(Exception error, int operationId) {
        onLoadingMessageDispatched.dispatch(new NetworkErrorMessage(operationId, error));
    }

    @Override
    public void onParsingErrorHappened(Exception exception, int operationId) {
        onLoadingMessageDispatched.dispatch(new ParsingErrorMessage(operationId));
    }

    @Override
    public void onLoadingDelegated(int operationId) {
        onLoadingMessageDispatched.dispatch(new TerminalMessage(operationId));
    }

    @Override
    public void onPartiallyCachedResultsFetched(CachedLessonsSet cacheSet) {
        onLoadingMessageDispatched.dispatch(new FinishedLoadingFromCacheMessage());
        onPartiallyCachedResultsFetched.dispatch(cacheSet);
    }

    @Override
    public void onNothingFoundInCache() {
        onLoadingMessageDispatched.dispatch(new FinishedLoadingFromCacheMessage());
    }

    @Override
    public void onLoadingAborted(int operationId) {
        //Technically it should never happen here since each request retries infinite times
        onLoadingMessageDispatched.dispatch(new TerminalMessage(operationId));
    }
}
