package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew

abstract class WaitForDownloadLessonListener : LessonsLoadingListener {

    private var numRequestsSent: Int = 0
    private var numRequestsSucceeded: Int = 0
    private var numRequestsFailed: Int = 0

    init {
        this.numRequestsSent = 0
        this.numRequestsSucceeded = 0
        this.numRequestsFailed = 0
    }

    override fun onLoadingMessageDispatched(operation: ILoadingMessage) {
        numRequestsSent++
    }

    override fun onLessonsLoaded(lessonsSet: List<LessonSchedule>, interval: List<LessonTypeNew>, operationId: Int) {
        numRequestsSucceeded++
        checkIfWeHaveFinished()
    }

    override fun onNetworkErrorHappened(error: Exception, operationId: Int) = //Managed in onLoadingAborted
            Unit

    override fun onParsingErrorHappened(exception: Exception, operationId: Int) = //Managed in onLoadingAborted
            Unit

    override fun onNothingFoundInCache() = //Nothing to do
            Unit

    override fun onLoadingAborted(operationId: Int) {
        numRequestsFailed++
        checkIfWeHaveFinished()
    }

    fun onNothingToLoad() = onFinish(true)

    private fun checkIfWeHaveFinished() {
        if (numRequestsSent == numRequestsSucceeded + numRequestsFailed) {

            val withSuccess = numRequestsSent == numRequestsSucceeded
            onFinish(withSuccess)
        }
    }

    abstract fun onFinish(withSuccess: Boolean)

}
