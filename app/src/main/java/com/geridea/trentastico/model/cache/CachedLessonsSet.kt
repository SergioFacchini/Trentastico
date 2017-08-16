package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.LessonsSet
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.WeekInterval

import java.util.ArrayList

class CachedLessonsSet : LessonsSet() {

    /**
     * @return the intervals that we were unable to load from cache.
     */
    val missingIntervals = ArrayList<NotCachedInterval>()
    val cachedIntervals = ArrayList<CachedInterval>()

    fun addCachedLessonTypes(cachedLessonTypes: ArrayList<CachedLessonType>) {
        for (cachedLessonType in cachedLessonTypes) {

            val isVisible = !AppPreferences.hasLessonTypeWithIdHidden(cachedLessonType.lesson_type_id.toLong())
            addLessonType(LessonType(cachedLessonType, isVisible))
        }
    }

    fun addMissingIntervals(missingIntervals: ArrayList<NotCachedInterval>) {
        this.missingIntervals.addAll(missingIntervals)
    }

    fun hasMissingIntervals(): Boolean {
        return !missingIntervals.isEmpty()
    }

    fun wereSomeLessonsFoundInCache(): Boolean {
        return !scheduledLessons.isEmpty()
    }

    fun isIntervalPartiallyOrFullyCached(intervalToCheck: WeekInterval): Boolean {
        for (cachedPeriod in cachedIntervals) {
            if (cachedPeriod.overlaps(intervalToCheck)) {
                return true
            }
        }

        return false
    }

    fun addCachedPeriod(cachedPeriod: CachedInterval) {
        cachedIntervals.add(cachedPeriod)
    }

    val cachedWeekIntervals: ArrayList<WeekInterval>
        get() {
            val cachedIntervals = ArrayList<WeekInterval>()
            for (cachedPeriod in this.cachedIntervals) {
                cachedIntervals.add(cachedPeriod)
            }

            return cachedIntervals
        }

    private fun getCachedCourseLessons(cachedPeriod: CachedPeriod): ArrayList<LessonSchedule> {
        val extraCourses = AppPreferences.extraCourses
        val lessonsToReturn = ArrayList<LessonSchedule>()
        for (lesson in scheduledLessons.values) {
            if (!extraCourses.isAnExtraLesson(lesson) && cachedPeriod.canContainStudyLesson(lesson)) {
                lessonsToReturn.add(lesson)
            }
        }

        return lessonsToReturn
    }

    private fun getCachedExtraLessons(cachedPeriod: CachedPeriod, extraCourse: ExtraCourse): ArrayList<LessonSchedule> {
        val lessonsToReturn = ArrayList<LessonSchedule>()
        for (lesson in scheduledLessons.values) {
            if (cachedPeriod.canContainExtraLesson(lesson, extraCourse)) {
                lessonsToReturn.add(lesson)
            }
        }

        return lessonsToReturn
    }
}
