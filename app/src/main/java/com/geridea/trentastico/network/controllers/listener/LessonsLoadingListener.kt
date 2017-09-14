package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew

interface LessonsLoadingListener {
    fun onLoadingMessageDispatched(operation: ILoadingMessage)
    fun onLessonsLoaded(lessons: List<LessonSchedule>, teachings: List<LessonTypeNew>, operationId: Int)
    fun onNetworkErrorHappened(error: Exception, operationId: Int)
    fun onParsingErrorHappened(exception: Exception, operationId: Int)

}
