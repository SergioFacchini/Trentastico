package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import org.json.JSONArray

import java.util.ArrayList

class ExtraCoursesList : ArrayList<ExtraCourse>() {

    fun isAnExtraLesson(lesson: LessonSchedule): Boolean {
        for (extraCourse in this) {
            if (extraCourse.lessonTypeId.toLong() == lesson.lessonTypeId) {
                return true
            }
        }

        return false
    }

    fun toJSON(): JSONArray {
        val jsonArray = JSONArray()

        for (course in this) {
            jsonArray.put(course.toJSON())
        }

        return jsonArray
    }

    fun removeHaving(courseId: Long, year: Int) {
        removeAll(getExtraCoursesHaving(courseId, year))
    }

    fun getExtraCoursesHaving(courseId: Long, year: Int): ArrayList<ExtraCourse> {
        val extraCourses = ArrayList<ExtraCourse>()
        for (extraCourse in this) {
            if (extraCourse.courseId == courseId || extraCourse.year == year) {
                extraCourses.add(extraCourse)
            }
        }

        return extraCourses
    }

    fun removeHavingLessonType(lessonTypeId: Int) {
        val iterator = this.iterator()
        while (iterator.hasNext()) {
            val course = iterator.next()
            if (course.lessonTypeId == lessonTypeId) {
                iterator.remove()
            }
        }
    }

    fun hasCourseWithId(lessonTypeId: Int): Boolean {
        for (extraCourse in this) {
            if (extraCourse.lessonTypeId == lessonTypeId) {
                return true
            }
        }
        return false
    }
}
