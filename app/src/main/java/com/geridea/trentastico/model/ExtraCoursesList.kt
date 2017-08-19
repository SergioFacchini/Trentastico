package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import org.json.JSONArray
import java.util.*

class ExtraCoursesList : ArrayList<ExtraCourse>() {

    fun isAnExtraLesson(lesson: LessonSchedule): Boolean =
            this.any { it.lessonTypeId.toLong() == lesson.lessonTypeId }

    fun toJSON(): JSONArray {
        val jsonArray = JSONArray()

        for (course in this) {
            jsonArray.put(course.toJSON())
        }

        return jsonArray
    }

    fun removeHaving(studyCourse: StudyCourse) {
        removeAll(getExtraCoursesOfCourse(studyCourse))
    }

    fun getExtraCoursesOfCourse(studyCourse: StudyCourse): ArrayList<ExtraCourse> =
            filterTo(ArrayList()) { it.isPartOfCourse(studyCourse) }

    fun removeHavingLessonType(lessonTypeId: String) {
        val iterator = this.iterator()
        while (iterator.hasNext()) {
            val course = iterator.next()
            if (course.lessonTypeId == lessonTypeId) {
                iterator.remove()
            }
        }
    }

    fun hasCourseWithId(lessonTypeId: String): Boolean = any { it.lessonTypeId == lessonTypeId }

}
