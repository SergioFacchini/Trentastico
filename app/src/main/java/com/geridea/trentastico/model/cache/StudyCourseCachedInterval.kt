package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener
import com.geridea.trentastico.utils.time.WeekInterval

import java.util.ArrayList

class StudyCourseCachedInterval(interval: WeekInterval, private val studyCourse: StudyCourse, private val cachedLessons: ArrayList<LessonSchedule>) : CachedInterval(interval) {

    override fun launchDiffRequest(controller: LessonsController, listener: LessonsDifferenceListener) {
        controller.diffStudyCourseLessons(this, studyCourse, cachedLessons, listener)
    }
}
