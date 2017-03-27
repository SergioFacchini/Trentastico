package com.geridea.trentastico.network;

import com.android.volley.VolleyError;
import com.geridea.trentastico.database.NotCachedInterval;
import com.geridea.trentastico.gui.views.ScrollDirection;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.operations.ILoadingOperation;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal2;

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
    public final Signal2<LessonsSet, WeekInterval> onLoadingOperationSuccessful = new Signal2<>();

    /**
     * Dispatched when the calendar starts loading something from internet. The argument is that
     * "something".
     */
    public final Signal1<ILoadingOperation> onLoadingOperationStarted = new Signal1<>();

    /**
     * Dispatched when an error happened when trying to fetch lessons.
     */
    public final Signal1<VolleyError> onLoadingErrorHappened = new Signal1<>();

    /**
     * Dispatched when some results were retrieved from the cache, however some results are still
     * missing and will have to be loaded from network.
     */
    public final Signal1<CachedLessonsSet> onPartiallyCachedResultsFetched = new Signal1<>();

    /**
     * Dispatched when the received response could not be parsed correctly.
     */
    public final Signal1<Exception> onParsingErrorHappened = new Signal1<>();

    private List<WeekInterval> loadingIntervals = Collections.synchronizedList(new ArrayList<WeekInterval>());

    public LessonsLoader() {

    }

    public void loadAndAddLessons(final WeekInterval intervalToLoad) {
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
    public void onLoadingAboutToStart(ILoadingOperation loadingOperation) {
        onLoadingOperationStarted.dispatch(loadingOperation);
    }

    @Override
    public void onLessonsLoaded(LessonsSet lessons, WeekInterval interval) {
        removeLoadingInterval(interval);

        lessons.removeLessonsWithTypeIds(AppPreferences.getLessonTypesIdsToHide());
        lessons.removeLessonsWithHiddenPartitionings();
        onLoadingOperationSuccessful.dispatch(lessons, interval);
    }

    @Override
    public void onErrorHappened(VolleyError error) {
        onLoadingErrorHappened.dispatch(error);
    }

    @Override
    public void onParsingErrorHappened(Exception exception) {
        onParsingErrorHappened.dispatch(exception);
    }

    @Override
    public void onPartiallyCachedResultsFetched(CachedLessonsSet cacheSet) {
        cacheSet.removeLessonsWithTypeIds(AppPreferences.getLessonTypesIdsToHide());
        cacheSet.removeLessonsWithHiddenPartitionings();

        onPartiallyCachedResultsFetched.dispatch(cacheSet);
    }
}
