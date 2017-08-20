package com.geridea.trentastico.network.controllers.listener

import com.geridea.trentastico.model.Course


/*
 * Created with ♥ by Slava on 18/08/2017.
 */

/**
 * Listener used to keep track of the responses of the LoadStudyCoursesRequest
 */
interface CoursesLoadingListener {

    /**
     * Dispatched when the courses were successfully fetched
     */
    fun onCoursesFetched(courses: List<Course>)

    /**
     * Dispatched when a network-related error happens.
     */
    fun onLoadingError()

    /**
     * Dispatched when a network-related error happens.
     */
    fun onParsingError(exception: Exception)

}