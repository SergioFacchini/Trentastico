package com.geridea.trentastico.network

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.LibraryOpeningTimesController
import com.geridea.trentastico.network.controllers.SendSlavaController
import com.geridea.trentastico.network.controllers.listener.*
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarUtils
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

object Networker {

    //Executors
    private lateinit var lessonsController: LessonsController
    private lateinit var sendSlavaController: SendSlavaController
    private lateinit var libraryOpeningTimes: LibraryOpeningTimesController

    fun init(cacher: Cacher) {
        val requestSender = RequestSender()

        sendSlavaController = SendSlavaController(requestSender)
        libraryOpeningTimes = LibraryOpeningTimesController(requestSender, cacher)
        lessonsController   = LessonsController(requestSender, cacher)

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

    data class SyncLoadResult(val lessons: List<LessonSchedule>, val wereErrors: Boolean)

    fun syncLoadTodaysLessons(): SyncLoadResult {
        val listener = BlockingLessonsLoadingListener()
        loadLessons(listener)
        loadExtraCourses(listener)

        val listOfLessons = mutableListOf<LessonSchedule>()
        val numOfLessonsToTake = AppPreferences.extraCourses.count() + 1
        var wereErrors = false

        for (i in 0 until numOfLessonsToTake) {
            val lessons = listener.getLessons()
            if (lessons != null) {
                listOfLessons.addAll(lessons)
            } else {
                wereErrors = true
            }
        }

        val today = CalendarUtils.today()
        val todayLessons = listOfLessons.filter { CalendarUtils.isSameDay(it.startCal, today) }

        return SyncLoadResult(todayLessons, wereErrors)
    }

    class BlockingLessonsLoadingListener : LessonsLoadingListener {

        private var lessonsQueue = ArrayBlockingQueue<List<LessonSchedule>?>(50)

        override fun onLoadingMessageDispatched(operation: ILoadingMessage) {}

        override fun onLessonsLoaded(lessons: List<LessonSchedule>, teachings: List<LessonType>, operationId: Int) {
            lessonsQueue.add(lessons)
        }

        override fun onNetworkErrorHappened(error: Exception, operationId: Int) {
            lessonsQueue.add(null)
        }

        override fun onParsingErrorHappened(exception: Exception, operationId: Int) {
            lessonsQueue.add(null)
        }

        fun getLessons(): List<LessonSchedule>? = lessonsQueue.take()
    }

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
            listener: FeedbackSendListener) = sendSlavaController.sendFeedback(feedback, name, email, listener)

    //----------------------------
    // Library opening times
    //----------------------------
    fun getLibraryOpeningTimes(
            day: Calendar,
            listener: LibraryOpeningTimesListener) = libraryOpeningTimes.getLibraryOpeningTimes(day, listener)

    fun sendDonationNotify(
            itemName: String,
            who: String) = sendSlavaController.sendDonation(itemName, who)


}
