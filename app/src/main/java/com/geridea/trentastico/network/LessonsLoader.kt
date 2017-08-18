package com.geridea.trentastico.network

import com.geridea.trentastico.gui.views.ScrollDirection
import com.geridea.trentastico.gui.views.ScrollDirection.LEFT
import com.geridea.trentastico.gui.views.requestloader.*
import com.geridea.trentastico.model.LessonsSet
import com.geridea.trentastico.model.cache.CachedLessonsSet
import com.geridea.trentastico.model.cache.NotCachedInterval
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.utils.time.WeekInterval
import com.geridea.trentastico.utils.time.WeekTime
import com.threerings.signals.Signal1
import com.threerings.signals.Signal3
import java.util.*


class LessonsLoader : LessonsLoadingListener, LoadingIntervalKnownListener {

    /**
     * Dispatched when the loading of events has been completed and the calendar can be made
     * visible.
     */
    val onLoadingOperationSuccessful = Signal3<LessonsSet, WeekInterval, ILoadingMessage>()

    /**
     * Dispatched when some results were retrieved from the cache, however some results are still
     * missing and will have to be loaded from network.
     */
    val onPartiallyCachedResultsFetched = Signal1<CachedLessonsSet>()

    /**
     * Dispatched when the calendar starts loading something from internet. The argument is that
     * "something".
     */
    val onLoadingMessageDispatched = Signal1<ILoadingMessage>()

    private val loadingIntervals = Collections.synchronizedList(ArrayList<WeekInterval>())

    private fun loadAndAddLessons(intervalToLoad: WeekInterval) {
        addLoadingInterval(intervalToLoad)
        Networker.loadLessons(intervalToLoad, this, this)

        onLoadingMessageDispatched.dispatch(LoadingFromCacheMessage(intervalToLoad))
    }

    override fun onIntervalsToLoadKnown(originalInterval: WeekInterval, intervals: ArrayList<NotCachedInterval>) {
        addLoadingIntervals(intervals)
        removeLoadingInterval(originalInterval)
    }

    private fun addLoadingIntervals(intervalsToLoad: ArrayList<NotCachedInterval>) {
        for (interval in intervalsToLoad) {
            addLoadingInterval(interval)
        }
    }

    private fun addLoadingInterval(intervalToLoad: WeekInterval) {
        synchronized(loadingIntervals) {
            loadingIntervals.add(intervalToLoad)
        }
    }

    private fun removeLoadingInterval(intervalToRemove: WeekInterval) {
        synchronized(loadingIntervals) {
            loadingIntervals.remove(intervalToRemove)
        }
    }

    private fun isWeekAlreadyBeingLoaded(week: WeekTime): Boolean = synchronized(loadingIntervals) {
        for (loadingInterval in loadingIntervals) {
            if (loadingInterval.contains(week)) {
                return true
            }
        }
        return false
    }

    fun loadDaysIfNeeded(weekToLoad: WeekTime, direction: ScrollDirection) {
        if (!isWeekAlreadyBeingLoaded(weekToLoad)) {
            val loadFrom: WeekTime
            val loadTo: WeekTime
            if (direction == LEFT) {
                loadFrom = weekToLoad.copy()
                loadFrom.addWeeks(-2)

                loadTo = weekToLoad.copy()
                loadTo.addWeeks(+1)
            } else {
                //We scrolled forward
                loadFrom = weekToLoad.copy()

                loadTo = loadFrom.copy()
                loadTo.addWeeks(+2)
            }

            loadAndAddLessons(WeekInterval(loadFrom, loadTo))
        }
    }

    fun loadEventsNearDay(day: Calendar) {
        val fromWT = WeekTime(day)
        fromWT.addWeeks(-1)

        val toWT = WeekTime(day)
        toWT.addWeeks(+2)

        loadAndAddLessons(WeekInterval(fromWT, toWT))
    }

    override fun onLoadingAboutToStart(loadingOperation: ILoadingMessage) = onLoadingMessageDispatched.dispatch(loadingOperation)

    override fun onLessonsLoaded(lessons: LessonsSet, interval: WeekInterval, operationId: Int) {
        //We might have gotten this lessons from cache:
        onLoadingMessageDispatched.dispatch(FinishedLoadingFromCacheMessage())

        removeLoadingInterval(interval)

        //Operation id is null when we loaded the lesson without starting any new request
        //(example: fetched from cache)
        val message = if (operationId == 0) NoOperationMessage() else TerminalMessage(operationId)

        onLoadingOperationSuccessful.dispatch(lessons, interval, message)
    }

    override fun onErrorHappened(error: Exception, operationId: Int) = onLoadingMessageDispatched.dispatch(NetworkErrorMessage(operationId, error))

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) = onLoadingMessageDispatched.dispatch(ParsingErrorMessage(operationId))

    override fun onLoadingDelegated(operationId: Int) = onLoadingMessageDispatched.dispatch(TerminalMessage(operationId))

    override fun onPartiallyCachedResultsFetched(cacheSet: CachedLessonsSet) {
        onLoadingMessageDispatched.dispatch(FinishedLoadingFromCacheMessage())
        onPartiallyCachedResultsFetched.dispatch(cacheSet)
    }

    override fun onNothingFoundInCache() = onLoadingMessageDispatched.dispatch(FinishedLoadingFromCacheMessage())

    override fun onLoadingAborted(operationId: Int) = //Technically it should never happen here since each request retries infinite times
            onLoadingMessageDispatched.dispatch(TerminalMessage(operationId))
}
