package com.geridea.trentastico.model

import java.util.ArrayList

/*
 * Created with ♥ by Slava on 03/03/2017.
 */
class Department(val id: Int, val name: String) {

    val courses = ArrayList<Course>()

    fun addCourse(course: Course) {
        courses.add(course)
    }

    fun getCoursePosition(courseId: Long): Int {
        for (i in courses.indices) {
            if (courses[i].id.toLong() == courseId) {
                return i
            }
        }

        throw RuntimeException("Unknown course with id: " + courseId)
    }

    fun getCourseWithId(courseId: Long): Course {
        return courses[getCoursePosition(courseId)]
    }
}
