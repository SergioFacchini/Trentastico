package com.geridea.trentastico.model

import com.geridea.trentastico.providers.DepartmentsProvider
import java.util.*

class StudyCourse(val departmentId: Long, val courseId: Long, year: Int) {
    var year: Int = 0
        private set

    init {
        this.year = year
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is StudyCourse) {
            val course = obj as StudyCourse?
            return course!!.departmentId == this.departmentId &&
                    course.courseId == this.courseId &&
                    course.year == this.year
        }

        return false
    }

    /**
     * Tries to decrease the year if possible; if not increases it by one. Used to get the previous
     * year's study course if possible.
     */
    fun decreaseOrChangeYear() {
        if (year == 1) {
            year++
        } else {
            year--
        }
    }

    fun generateFullDescription(): String {
        val department = DepartmentsProvider.getDepartmentWithId(departmentId)
        val course = department.getCourseWithId(courseId)

        return String.format(Locale.ITALY, "%s > %s - %d° anno",
                department.name, course.name, year
        )


    }

    val courseAndYear: CourseAndYear
        get() {
            val cay = CourseAndYear()
            cay.courseId = courseId
            cay.year = year
            return cay
        }

    override fun toString(): String = String.format("Department: %d - Course: %d - Year %d", departmentId, courseId, year)
}
