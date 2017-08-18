package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule
import java.util.*

class LessonsWithRoomMultipleListener
/**
 *
 * @param lessonsToLoad the lessons that need the room to be loaded
 * @param lessonsWithRooms lessons that already have rooms. The will just need to be dispatched
 * as they are, merged with the lessons that gained rooms.
 * @param listener the listener
 */
(private val lessonsThatWillBeUpdated: ArrayList<LessonSchedule>, private val lessonsWithRooms: ArrayList<LessonSchedule>, private val listener: LessonsWithRoomListener) : LessonWithRoomFetchedListener {

    private var numOfRequestReceived: Int = 0
    private val lessonsWithoutRooms: ArrayList<LessonSchedule> = ArrayList()

    override fun onUpdateSuccessful(updatedLessons: LessonSchedule) = updateCounterAndCheckIfCompleted()

    private fun updateCounterAndCheckIfCompleted() {
        numOfRequestReceived++
        if (numOfRequestReceived == lessonsThatWillBeUpdated.size) {
            lessonsThatWillBeUpdated.addAll(lessonsWithRooms)
            listener.onLoadingCompleted(lessonsThatWillBeUpdated, lessonsWithoutRooms)
        }
    }

    override fun onError(lesson: LessonSchedule) {
        lessonsWithoutRooms.add(lesson)

        updateCounterAndCheckIfCompleted()
    }

}
