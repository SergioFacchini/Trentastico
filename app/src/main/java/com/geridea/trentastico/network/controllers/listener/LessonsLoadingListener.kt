package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonsSet
import com.geridea.trentastico.model.cache.CachedLessonsSet
import com.geridea.trentastico.utils.time.WeekInterval

interface LessonsLoadingListener {
    fun onLoadingAboutToStart(operation: ILoadingMessage)
    fun onLessonsLoaded(lessonsSet: LessonsSet, interval: WeekInterval, operationId: Int)
    fun onErrorHappened(error: Exception, operationId: Int)
    fun onParsingErrorHappened(exception: Exception, operationId: Int)
    fun onLoadingDelegated(operationId: Int)
    fun onPartiallyCachedResultsFetched(lessonsSet: CachedLessonsSet)
    fun onNothingFoundInCache()

    /**
     * Called when the loading of the request has been aborted. Can happen when the request could
     * not be fetched and it doesn't allow retrials.
     */
    fun onLoadingAborted(operationId: Int)


}
