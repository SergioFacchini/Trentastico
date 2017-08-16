package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonsSet
import com.geridea.trentastico.model.cache.CachedLessonsSet
import com.geridea.trentastico.utils.time.WeekInterval

abstract class WaitForDownloadLessonListener : LessonsLoadingListener {

    private var numRequestsSent: Int = 0
    private var numRequestsSucceeded: Int = 0
    private var numRequestsFailed: Int = 0

    init {
        this.numRequestsSent = 0
        this.numRequestsSucceeded = 0
        this.numRequestsFailed = 0
    }

    override fun onLoadingAboutToStart(operation: ILoadingMessage) {
        numRequestsSent++
    }

    override fun onLessonsLoaded(lessonsSet: LessonsSet, interval: WeekInterval, operationId: Int) {
        numRequestsSucceeded++
        checkIfWeHaveFinished()
    }

    override fun onErrorHappened(error: Exception, operationId: Int) {
        //Managed in onLoadingAborted
    }

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) {
        //Managed in onLoadingAborted
    }

    override fun onLoadingDelegated(operationId: Int) {
        //Should never happen since we do not manage loading from cache
        BugLogger.logBug("Cache used when shouldn't", RuntimeException("Cache used when shouldn't"))
    }

    override fun onPartiallyCachedResultsFetched(lessonsSet: CachedLessonsSet) {
        //Should never happen since we do not manage loading from cache
        BugLogger.logBug("Cache used when shouldn't", RuntimeException("Cache used when shouldn't"))
    }

    override fun onNothingFoundInCache() {
        //Nothing to do
    }

    override fun onLoadingAborted(operationId: Int) {
        numRequestsFailed++
        checkIfWeHaveFinished()
    }

    fun onNothingToLoad() {
        onFinish(true)
    }

    private fun checkIfWeHaveFinished() {
        if (numRequestsSent == numRequestsSucceeded + numRequestsFailed) {

            val withSuccess = numRequestsSent == numRequestsSucceeded
            onFinish(withSuccess)
        }
    }

    abstract fun onFinish(withSuccess: Boolean)

}
