package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 04/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule

import java.util.ArrayList

interface TodaysLessonsListener {
    fun onLessonsAvailable(lessons: ArrayList<LessonSchedule>)
    fun onLessonsCouldNotBeLoaded()
}
