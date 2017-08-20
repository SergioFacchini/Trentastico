package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew

interface LessonsLoadingListener {
    fun onLoadingAboutToStart(operation: ILoadingMessage)
    fun onLessonsLoaded(lessons: List<LessonSchedule>, teachings: List<LessonTypeNew>, operationId: Int)
    fun onNetworkErrorHappened(error: Exception, operationId: Int)
    fun onParsingErrorHappened(exception: Exception, operationId: Int)
    fun onNothingFoundInCache()

    /**
     * Called when the loading of the request has been aborted. Can happen when the request could
     * not be fetched and it doesn't allow retrials.
     */
    fun onLoadingAborted(operationId: Int)


}
