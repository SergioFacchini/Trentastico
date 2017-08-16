package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.network.request.LessonsDiffResult

interface LessonsDifferenceListener {

    fun onLoadingError()

    fun onRequestCompleted()

    fun onNumberOfRequestToSendKnown(numRequests: Int)

    fun onNoLessonsInCache()

    fun onDiffResult(lessonsDiffResult: LessonsDiffResult)
}
