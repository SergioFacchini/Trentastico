package com.geridea.trentastico.network

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.LibraryOpeningTimesController
import com.geridea.trentastico.network.controllers.SendFeedbackController
import com.geridea.trentastico.network.controllers.listener.*
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.utils.AppPreferences
import java.util.*

object Networker {

    //Executors
    private lateinit var lessonsController: LessonsController
    private lateinit var sendFeedbackController: SendFeedbackController
    private lateinit var libraryOpeningTimes: LibraryOpeningTimesController

    fun init(cacher: Cacher) {
        val requestSender = RequestSender()

        sendFeedbackController = SendFeedbackController(requestSender)
        libraryOpeningTimes    = LibraryOpeningTimesController(requestSender, cacher)
        lessonsController = LessonsController(requestSender, cacher)

    }

    //----------------------------
    // Study courses
    //----------------------------
    fun loadStudyCourses(listener: CoursesLoadingListener) =
            lessonsController.loadStudyCourses(listener)

    //----------------------------
    // Lessons
    //----------------------------

    fun loadLessons(listener: LessonsLoadingListener) =
            lessonsController.loadStandardLessons(listener, AppPreferences.studyCourse)

    fun loadExtraCourses(lessonsLoader: LessonsLoadingListener) {
        AppPreferences.extraCourses.forEach {
            lessonsController.loadExtraCourseLessons(lessonsLoader, it)
        }
    }

    fun syncDiffStudyCourseLessonsWithCachedOnes(
            lastValidTimestamp: Long,
            listener: DiffLessonsListener) {

        lessonsController.syncDiffStudyCourseLessonsWithCachedOnes(lastValidTimestamp, listener)
    }

    fun diffExtraCourseLessonsWithCachedOnes(
            extraCourse: ExtraCourse,
            lastValidTimestamp: Long? = null,
            listener: DiffLessonsListener) {

        lessonsController.diffExtraCourseLessonsWithCachedOnes(extraCourse, lastValidTimestamp, listener)
    }

    /**
     * Loads lesson types that are currently cached. The list of the callback is empty if there are
     * no cached lesson types.
     */
    fun loadCachedLessonTypes(callback: (List<LessonType>) -> Unit) {
        lessonsController.loadCachedLessonTypes(callback)
    }

    fun loadLessonTypesOfStudyCourse(
            studyCourse: StudyCourse,
            listener: ListLessonsListener) = lessonsController.loadLessonTypesOfStudyCourse(studyCourse, listener)

    fun loadTodaysCachedLessons(todaysLessonsListener: TodaysLessonsListener) =
            lessonsController.loadTodaysCachedLessons(todaysLessonsListener)

    fun syncLoadTodaysCachedLessons() =
            lessonsController.syncLoadTodaysCachedLessons()

    /**
     * Removes all the cached extra lessons of the lesson type having the given id
     */
    fun purgeExtraCourse(lessonTypeId: String) = lessonsController.removeExtraCoursesWithLessonType(lessonTypeId)

    /**
     * Deletes all the cache about lessons and lesson types.
     */
    fun obliterateCache() = lessonsController.obliterateCache()

    /**
     * Deletes all the cache about the currently chosen study course
     */
    fun purgeStudyCourseCache() = lessonsController.purgeStudyCourseCache()

    //----------------------------
    // Feedback
    //----------------------------
    fun sendFeedback(
            feedback: String,
            name: String,
            email: String,
            listener: FeedbackSendListener) = sendFeedbackController.sendFeedback(feedback, name, email, listener)

    //----------------------------
    // Library opening times
    //----------------------------
    fun getLibraryOpeningTimes(
            day: Calendar,
            listener: LibraryOpeningTimesListener) = libraryOpeningTimes.getLibraryOpeningTimes(day, listener)

}
