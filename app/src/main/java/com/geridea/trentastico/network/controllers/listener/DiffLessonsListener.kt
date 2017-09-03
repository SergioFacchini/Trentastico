package com.geridea.trentastico.network.controllers.listener

import com.geridea.trentastico.network.request.LessonsDiffResult


/*
 * Created with ♥ by Slava on 01/09/2017.
 */

/**
 * Listener that gets dispatched as a result of a diff request
 */
interface DiffLessonsListener {

    /**
     * Dispatched when the lessons were successfully fetched and diffed with the ones that were in
     * cache
     */
    fun onLessonsDiffed(result: LessonsDiffResult)

    /**
     * Dispatched when could not fetch fresh lessons to compare the cached ones to
     */
    fun onLessonsLoadingError()

    /**
     * Dispatched when there are no cached lessons to compare the new lessons to
     */
    fun onNoCachedLessons()

    /**
     * Dispatched after the request gets completed (not necessarily succeeded), but before any
     * other function of this listener is called
     */
    fun onBeforeRequestFinished()

}