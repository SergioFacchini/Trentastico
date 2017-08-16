package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.model.LessonType

interface ListLessonsListener {
    fun onErrorHappened(error: Exception)

    fun onParsingErrorHappened(e: Exception)

    fun onLessonTypesRetrieved(lessonTypes: Collection<LessonType>)
}
