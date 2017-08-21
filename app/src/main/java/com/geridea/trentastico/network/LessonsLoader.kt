package com.geridea.trentastico.network

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.gui.views.requestloader.NetworkErrorMessage
import com.geridea.trentastico.gui.views.requestloader.ParsingErrorMessage
import com.geridea.trentastico.gui.views.requestloader.TerminalMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.utils.AppPreferences
import com.threerings.signals.Signal1
import com.threerings.signals.Signal3


class LessonsLoader : LessonsLoadingListener {

    /**
     * Dispatched when the loading of events has been completed and the calendar can be made
     * visible.
     */
    val onLoadingOperationSuccessful = Signal3<List<LessonSchedule>, List<LessonTypeNew>, ILoadingMessage>()

    /**
     * Dispatched when the calendar starts loading something from internet. The argument is that
     * "something".
     */
    val onLoadingMessageDispatched = Signal1<ILoadingMessage>()

    fun loadAndAddLessons() {
        Networker.loadLessons(this)
        Networker.loadExtraCourses(this)
    }

    override fun onLoadingAboutToStart(operation: ILoadingMessage)
            = onLoadingMessageDispatched.dispatch(operation)

    override fun onLessonsLoaded(lessons: List<LessonSchedule>, teachings: List<LessonTypeNew>, operationId: Int) {
        //Filtering lessons that should not be shown
        val visibleLessons =
                lessons.filterNot { AppPreferences.lessonTypesToHideIds.contains(it.lessonTypeId) }

        onLoadingOperationSuccessful.dispatch(visibleLessons, teachings, TerminalMessage(operationId))
    }

    override fun onNetworkErrorHappened(error: Exception, operationId: Int) =
            onLoadingMessageDispatched.dispatch(NetworkErrorMessage(operationId, error))

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) =
            onLoadingMessageDispatched.dispatch(ParsingErrorMessage(operationId))

    override fun onNothingFoundInCache() = Unit


    override fun onLoadingAborted(operationId: Int) = //Technically it should never happen here since each request retries infinite times
            onLoadingMessageDispatched.dispatch(TerminalMessage(operationId))
}
