package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonsSet
import com.geridea.trentastico.model.cache.CachedLessonsSet
import com.geridea.trentastico.utils.time.WeekInterval

class DiffCompletedListener(private val differenceListener: LessonsDifferenceListener) : LessonsLoadingListener {

    override fun onLoadingAboutToStart(operation: ILoadingMessage) = //Nothing to do
            Unit

    override fun onLessonsLoaded(lessonsSet: LessonsSet, interval: WeekInterval, operationId: Int) = differenceListener.onRequestCompleted()

    override fun onErrorHappened(error: Exception, operationId: Int) = //Managed in onLoadingAborted
            Unit

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) = //Managed in onLoadingAborted
            Unit

    override fun onLoadingDelegated(operationId: Int) = //We disabled the cache, this should never happen
            BugLogger.logBug("Cache used when shouldn't", RuntimeException("Cache used when shouldn't"))

    override fun onPartiallyCachedResultsFetched(lessonsSet: CachedLessonsSet) = //We disabled the cache, this should never happen
            BugLogger.logBug("Cache used when shouldn't", RuntimeException("Cache used when shouldn't"))

    override fun onNothingFoundInCache() = //Nothing to do
            Unit

    override fun onLoadingAborted(operationId: Int) = differenceListener.onLoadingError()
}
