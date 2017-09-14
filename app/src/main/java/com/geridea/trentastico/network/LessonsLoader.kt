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

    /**
     * The number of requests that are being loaded right now.
     */
    var numRequestsBeingLoaded = 0

    /**
     * @return true if there is a pending network request, false otherwise
     */
    val hasAnythingBeingLoaded: Boolean
        get() = numRequestsBeingLoaded != 0

    fun loadAndAddLessons() {
        numRequestsBeingLoaded++
        Networker.loadLessons(this)

        numRequestsBeingLoaded += AppPreferences.extraCourses.size
        Networker.loadExtraCourses(this)
    }

    override fun onLoadingMessageDispatched(operation: ILoadingMessage)
            = onLoadingMessageDispatched.dispatch(operation)

    override fun onLessonsLoaded(lessons: List<LessonSchedule>, teachings: List<LessonTypeNew>, operationId: Int) {
        numRequestsBeingLoaded--

        //Filtering lessons that should not be shown
        val visibleLessons =
                lessons.filterNot { AppPreferences.lessonTypesToHideIds.contains(it.lessonTypeId) }

        onLoadingOperationSuccessful.dispatch(visibleLessons, teachings, TerminalMessage(operationId))
    }

    override fun onNetworkErrorHappened(error: Exception, operationId: Int) =
            onLoadingMessageDispatched.dispatch(NetworkErrorMessage(operationId, error))

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) =
            onLoadingMessageDispatched.dispatch(ParsingErrorMessage(operationId))

}
