package com.geridea.trentastico.network;

import com.geridea.trentastico.database.NotCachedInterval;
import com.geridea.trentastico.gui.views.ScrollDirection;
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.gui.views.requestloader.NoOperationMessage;
import com.geridea.trentastico.gui.views.requestloader.TerminalMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.operations.NetworkErrorMessage;
import com.geridea.trentastico.network.operations.ParsingErrorMessage;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static com.geridea.trentastico.gui.views.ScrollDirection.LEFT;


public class LessonsLoader implements LessonsLoadingListener {

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

    private List<WeekInterval> loadingIntervals = Collections.synchronizedList(new ArrayList<WeekInterval>());

    public LessonsLoader() {

    }

    private void loadAndAddLessons(final WeekInterval intervalToLoad) {
        ArrayList<NotCachedInterval> intervalsToLoad = Networker.loadLessons(intervalToLoad, this);
        addLoadingIntervals(intervalsToLoad);
    }

    private void addLoadingIntervals(ArrayList<NotCachedInterval> intervalsToLoad) {
        if (intervalsToLoad != null) {
            for (WeekInterval interval : intervalsToLoad) {
                addLoadingInterval(interval);
            }
        }
    }

    private void addLoadingInterval(WeekInterval intervalToLoad) {
        loadingIntervals.add(intervalToLoad);
    }

    private void removeLoadingInterval(WeekInterval intervalToRemove) {
        WeekInterval intervalToDelete = null;
        for (WeekInterval loadingInterval : loadingIntervals) {
            if(loadingInterval.equals(intervalToRemove)) {
                intervalToDelete = loadingInterval;
                break;
            }
        }

        loadingIntervals.remove(intervalToDelete);
    }

    private boolean isWeekAlreadyBeingLoaded(WeekTime week) {
        for (WeekInterval loadingInterval : loadingIntervals) {
            if (loadingInterval.contains(week)) {
                return true;
            }
        }
        return false;
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
        removeLoadingInterval(interval);

        lessons.removeLessonsWithTypeIds(AppPreferences.getLessonTypesIdsToHide());
        lessons.removeLessonsWithHiddenPartitionings();

        //Operation id is null when we loaded the lesson without starting any new request
        //(example: fetched from cache)
        ILoadingMessage message = (operationId == 0) ? new NoOperationMessage() : new TerminalMessage(operationId);

        onLoadingOperationSuccessful.dispatch(lessons, interval, message);
    }

    @Override
    public void onErrorHappened(Exception error, int operationId) {
        if(!(error instanceof IOException)){
            error.printStackTrace();
            BugLogger.logBug();
        }

        onLoadingMessageDispatched.dispatch(new NetworkErrorMessage(operationId, error));
    }

    @Override
    public void onParsingErrorHappened(Exception exception, int operationId) {
        onLoadingMessageDispatched.dispatch(new ParsingErrorMessage(operationId));

        BugLogger.logBug();
    }

    @Override
    public void onLoadingDelegated(int operationId) {
        onLoadingMessageDispatched.dispatch(new TerminalMessage(operationId));
    }

    @Override
    public void onPartiallyCachedResultsFetched(CachedLessonsSet cacheSet) {
        cacheSet.removeLessonsWithTypeIds(AppPreferences.getLessonTypesIdsToHide());
        cacheSet.removeLessonsWithHiddenPartitionings();

        onPartiallyCachedResultsFetched.dispatch(cacheSet);
    }
}
