package com.geridea.trentastico.network

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.database_new.CacherNew
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.controllers.LessonsControllerNew
import com.geridea.trentastico.network.controllers.LibraryOpeningTimesController
import com.geridea.trentastico.network.controllers.SendFeedbackController
import com.geridea.trentastico.network.controllers.listener.*
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.utils.AppPreferences
import java.util.*

object Networker {

    //Executors
    private lateinit var lessonsControllerNew: LessonsControllerNew
    private lateinit var sendFeedbackController: SendFeedbackController
    private lateinit var libraryOpeningTimes: LibraryOpeningTimesController

    fun init(cacherNew: CacherNew) {
        val requestSender = RequestSender()

        sendFeedbackController = SendFeedbackController(requestSender)
        libraryOpeningTimes    = LibraryOpeningTimesController(requestSender, cacherNew)
        lessonsControllerNew   = LessonsControllerNew(requestSender, cacherNew)

    }

    //----------------------------
    // Study courses
    //----------------------------
    fun loadStudyCourses(listener: CoursesLoadingListener) =
            lessonsControllerNew.loadStudyCourses(listener)

    //----------------------------
    // Lessons
    //----------------------------

    fun loadLessons(listener: LessonsLoadingListener) =
            lessonsControllerNew.loadStandardLessons(listener, AppPreferences.studyCourse)

    fun loadExtraCourses(lessonsLoader: LessonsLoadingListener) {
        AppPreferences.extraCourses.forEach {
            lessonsControllerNew.loadExtraCourseLessons(lessonsLoader, it)
        }
    }

    fun diffStudyCourseLessonsWithCachedOnes(
            lastValidTimestamp: Long,
            listener: DiffLessonsListener) {

        lessonsControllerNew.diffStudyCourseLessonsWithCachedOnes(lastValidTimestamp, listener)
    }

    fun diffExtraCourseLessonsWithCachedOnes(
            extraCourse: ExtraCourse,
            lastValidTimestamp: Long? = null,
            listener: DiffLessonsListener) {

        lessonsControllerNew.diffExtraCourseLessonsWithCachedOnes(extraCourse, lastValidTimestamp, listener)
    }

    /**
     * Loads lesson types that are currently cached. The list of the callback is empty if there are
     * no cached lesson types.
     */
    fun loadCachedLessonTypes(callback: (List<LessonTypeNew>) -> Unit) {
        lessonsControllerNew.loadCachedLessonTypes(callback)
    }

    fun loadLessonTypesOfStudyCourse(
            studyCourse: StudyCourse,
            listener: ListLessonsListener) = lessonsControllerNew.loadLessonTypesOfStudyCourse(studyCourse, listener)

    fun loadTodaysCachedLessons(todaysLessonsListener: TodaysLessonsListener) =
            lessonsControllerNew.loadTodaysCachedLessons(todaysLessonsListener)

    /**
     * Removes all the cached extra lessons of the lesson type having the given id
     */
    fun purgeExtraCourse(lessonTypeId: String) = lessonsControllerNew.removeExtraCoursesWithLessonType(lessonTypeId)

    /**
     * Deletes all the cache about lessons and lesson types.
     */
    fun obliterateCache() = lessonsControllerNew.obliterateCache()

    /**
     * Deletes all the cache about the currently chosen study course
     */
    fun purgeStudyCourseCache() = lessonsControllerNew.purgeStudyCourseCache()

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
