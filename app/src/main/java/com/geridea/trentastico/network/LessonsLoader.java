package com.geridea.trentastico.network;

import com.android.volley.VolleyError;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.operations.CalendarLoadingOperation;
import com.geridea.trentastico.network.operations.ILoadingOperation;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekDayTime;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LessonsLoader {

    /**
     * Dispatched when the loading of events has been completed and the calendar can be made
     * visible.
     */
    public final Signal2<LessonsSet, CalendarInterval> onLoadingOperationSuccessful = new Signal2<>();

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

    public void loadAndAddLessons(final WeekInterval intervalToLoad) {
        ArrayList<WeekInterval> intervalsToLoad = Networker.loadLessons(intervalToLoad, new LessonsFetchedListener() {
            @Override
            public void onLoadingAboutToStart(CalendarInterval interval) {
                onLoadingOperationStarted.dispatch(new CalendarLoadingOperation(interval));
            }

            @Override
            public void onLessonsLoaded(LessonsSet lessons, WeekInterval interval) {
                removeLoadingInterval(interval);

                onLoadingOperationSuccessful.dispatch(lessons, interval.toCalendarInterval());
            }

            @Override
            public void onErrorHappened(VolleyError error) {
                onLoadingErrorHappened.dispatch(error);
            }

            @Override
            public void onParsingErrorHappened(Exception e) {
                onParsingErrorHappened.dispatch(e);
            }

            @Override
            public void onPartiallyCachedResultsFetched(CachedLessonsSet cacheSet) {
                onPartiallyCachedResultsFetched.dispatch(cacheSet);
            }
        });

        addLoadingIntervals(intervalsToLoad);
    }

    private void addLoadingIntervals(ArrayList<WeekInterval> intervalsToLoad) {
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

    private boolean isDayAlreadyBeingLoaded(WeekDayTime day) {
        for (WeekInterval loadingInterval : loadingIntervals) {
            if (loadingInterval.contains(day)) {
                return true;
            }
        }
        return false;
    }

    public void loadDayOnDayChangeIfNeeded(WeekDayTime newFirstVisibleDay, WeekDayTime oldFirstVisibleDay) {
        if (!isDayAlreadyBeingLoaded(newFirstVisibleDay)) {
            WeekTime loadFrom, loadTo;
            if(newFirstVisibleDay.before(oldFirstVisibleDay)){
                //We scrolled backwards
                loadFrom = new WeekTime(newFirstVisibleDay);
                loadFrom.addWeeks(-1);

                loadTo = new WeekTime(oldFirstVisibleDay);
            } else {
                //We scrolled forward
                loadFrom = new WeekTime(newFirstVisibleDay);

                loadTo = loadFrom.copy();
                loadTo.addWeeks(+2);
            }

            loadAndAddLessons(new WeekInterval(loadFrom, loadTo));
        }
    }

    public void loadNearEvents() {
        WeekTime fromWT = WeekTime.getCurrentInstance();
        fromWT.addWeeks(-1);

        WeekTime toWT = WeekTime.getCurrentInstance();
        toWT.addWeeks(+2);

        loadAndAddLessons(new WeekInterval(fromWT, toWT));
    }
}
