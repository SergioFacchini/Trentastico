package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule

interface LessonWithRoomFetchedListener {
    fun onUpdateSuccessful(lesson: LessonSchedule)
    fun onError(lesson: LessonSchedule)
}
