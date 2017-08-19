package com.geridea.trentastico.network

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.LessonsControllerNew
import com.geridea.trentastico.network.controllers.LibraryOpeningTimesController
import com.geridea.trentastico.network.controllers.SendFeedbackController
import com.geridea.trentastico.network.controllers.listener.*
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.utils.time.WeekInterval
import java.util.*

object Networker {

    //Executors
    private lateinit var lessonsController: LessonsController
    private lateinit var lessonsControllerNew: LessonsControllerNew
    private lateinit var sendFeedbackController: SendFeedbackController
    private lateinit var libraryOpeningTimes: LibraryOpeningTimesController

    fun init(cacher: Cacher) {
        val requestSender = RequestSender()

        sendFeedbackController = SendFeedbackController(requestSender, cacher)
        libraryOpeningTimes    = LibraryOpeningTimesController(requestSender, cacher)
        lessonsController      = LessonsController(requestSender, cacher)
        lessonsControllerNew   = LessonsControllerNew(requestSender, cacher)

    }

    //----------------------------
    // Lessons
    //----------------------------

    /**
     * Loads the lessons in the given period. Fetches all the possible lesson from the fresh cache
     * or dead cache and the remaining from internet.
     * @param interval interval to load
     * @param listener listener that will receive the results
     * @param intervalListener the listener that will be dispatched once we know what intervals have
     * to be loaded
     */
    fun loadLessons(
            interval: WeekInterval,
            listener: LessonsLoadingListener,
            intervalListener: LoadingIntervalKnownListener) = lessonsController.loadLessons(interval, listener, intervalListener)

    fun diffLessonsInCache(
            intervalToCheck: WeekInterval,
            listener: LessonsDifferenceListener) = lessonsController.diffLessonsInCache(intervalToCheck, listener)

    fun loadAndCacheNotCachedLessons(
            interval: WeekInterval,
            listener: WaitForDownloadLessonListener) = lessonsController.loadAndCacheNotCachedLessons(interval, listener)

    fun loadCoursesOfStudyCourse(
            studyCourse: StudyCourse,
            listener: ListLessonsListener) = lessonsController.loadCoursesOfStudyCourse(studyCourse, listener)

    fun loadTodaysLessons(todaysLessonsListener: TodaysLessonsListener) = lessonsController.loadTodaysLessons(todaysLessonsListener)

    fun getTodaysCachedLessons(todaysLessonsListener: TodaysLessonsListener) = lessonsController.getTodaysCachedLessons(todaysLessonsListener)

    /**
     * Removes all the extra courses and lessons having the given lesson type
     */
    fun removeExtraCoursesWithLessonType(lessonTypeId: String) = lessonsController.removeExtraCoursesWithLessonType(lessonTypeId)

    /**
     * Deletes all the cache about lessons and lesson types.
     */
    fun obliterateLessonsCache() = lessonsController.obliterateAllLessonsCache()

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

    fun loadStudyCourses(listener: CoursesLoadingListener) =
            lessonsControllerNew.loadStudyCourses(listener)

}
