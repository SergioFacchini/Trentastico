package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew

class DiffCompletedListener(private val differenceListener: LessonsDifferenceListener) : LessonsLoadingListener {

    override fun onLoadingAboutToStart(operation: ILoadingMessage) = //Nothing to do
            Unit

    override fun onLessonsLoaded(lessonsSet: List<LessonSchedule>, interval: List<LessonTypeNew>, operationId: Int) = differenceListener.onRequestCompleted()

    override fun onNetworkErrorHappened(error: Exception, operationId: Int) = //Managed in onLoadingAborted
            Unit

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) = //Managed in onLoadingAborted
            Unit

    override fun onNothingFoundInCache() = //Nothing to do
            Unit

    override fun onLoadingAborted(operationId: Int) = differenceListener.onLoadingError()
}
