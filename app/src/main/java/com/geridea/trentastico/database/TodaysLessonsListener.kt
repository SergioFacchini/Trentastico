package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 04/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule

interface TodaysLessonsListener {
    fun onLessonsAvailable(lessons: List<LessonSchedule>)
    fun onLessonsCouldNotBeLoaded()
}
