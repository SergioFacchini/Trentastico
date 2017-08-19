package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 13/04/2017.
 */
/**
 * Keeps track of the id of the course and a year of that course. Useful to identify a specific
 * study course.
 */
data class CourseAndYear(var courseId: String, var courseName: String, var year: StudyYear) {

    constructor(course: Course, year: StudyYear) : this(course.id, course.name, year)

}
