package com.geridea.trentastico.network.controllers


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import com.geridea.trentastico.Config
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.gui.views.requestloader.ExtraCoursesLoadingMessage
import com.geridea.trentastico.gui.views.requestloader.LessonsLoadingMessage
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.*
import com.geridea.trentastico.network.LoadingIntervalKnownListener
import com.geridea.trentastico.network.controllers.listener.*
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.network.request.ResponseUnsuccessfulException
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarInterval
import com.geridea.trentastico.utils.time.WeekDayTime
import com.geridea.trentastico.utils.time.WeekInterval
import okhttp3.FormBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class LessonsController(sender: RequestSender, cacher: Cacher) : BasicController(sender, cacher) {

    fun loadRoomsForLessonsIfMissing(
            lessonsToFilter: ArrayList<LessonSchedule>,
            listener: LessonsWithRoomListener) {
        //Finding lessons with and without rooms
        val lessonsToLoad = ArrayList<LessonSchedule>()
        val lessonsWithRoom = ArrayList<LessonSchedule>()
        for (lesson in lessonsToFilter) {
            if (lesson.hasRoomSpecified()) {
                lessonsWithRoom.add(lesson)
            } else {
                lessonsToLoad.add(lesson)
            }
        }

        //If there are no lessons to load, we just return them
        if (lessonsToLoad.isEmpty()) {
            listener.onLoadingCompleted(lessonsToFilter, ArrayList())
        } else {
            val fetchRoomListener = LessonsWithRoomMultipleListener(lessonsToLoad, lessonsWithRoom, listener)

            for (lesson in lessonsToLoad) {
                val cay = LessonSchedule.findCourseAndYearForLesson(lesson)
                sender.processRequest(FetchRoomForLessonRequest(lesson, cay, fetchRoomListener))
            }

        }
    }

    fun getTodaysCachedLessons(todaysLessonsListener: TodaysLessonsListener) {
        cacher.getTodaysCachedLessons(todaysLessonsListener)
    }

    fun loadLessons(
            intervalToLoad: WeekInterval, listener: LessonsLoadingListener,
            intervalListener: LoadingIntervalKnownListener) {

        val extraCourses = AppPreferences.extraCourses
        cacher.getLessonsInFreshOrDeadCacheAsync(intervalToLoad, extraCourses, false) { cacheSet ->
            if (cacheSet.hasMissingIntervals()) {
                if (cacheSet.wereSomeLessonsFoundInCache()) {
                    listener.onPartiallyCachedResultsFetched(cacheSet)
                } else {
                    listener.onNothingFoundInCache()
                }

                for (interval in cacheSet.missingIntervals) {
                    interval.launchLoading(this@LessonsController, listener)
                }
            } else {
                //We found everything we needed in cache
                listener.onLessonsLoaded(cacheSet, intervalToLoad, 0)
            }

            intervalListener.onIntervalsToLoadKnown(intervalToLoad, cacheSet.missingIntervals)
        }
    }

    fun diffLessonsInCache(intervalToCheck: WeekInterval, listener: LessonsDifferenceListener) {
        val extraCourses = AppPreferences.extraCourses
        cacher.getLessonsInFreshOrDeadCacheAsync(intervalToCheck, extraCourses, false) { cacheSet ->
            if (cacheSet.isIntervalPartiallyOrFullyCached(intervalToCheck)) {
                val cachedIntervals = cacheSet.cachedIntervals

                listener.onNumberOfRequestToSendKnown(cachedIntervals.size)

                for (cachedInterval in cachedIntervals) {
                    cachedInterval.launchDiffRequest(this@LessonsController, listener)
                }
            } else {
                listener.onNoLessonsInCache()
            }
        }
    }

    fun loadAndCacheNotCachedLessons(
            interval: WeekInterval,
            listener: WaitForDownloadLessonListener) {
        val extraCourses = AppPreferences.extraCourses
        cacher.getNotCachedSubintervals(interval, extraCourses) { notCachedIntervals ->
            if (notCachedIntervals.isEmpty()) {
                listener.onNothingToLoad()
            } else {
                for (notCachedInterval in notCachedIntervals) {
                    notCachedInterval.launchLoadingOneTime(this@LessonsController, listener)
                }
            }
        }
    }

    fun loadCoursesOfStudyCourse(studyCourse: StudyCourse, listener: ListLessonsListener) {
        sender.processRequest(ListLessonsRequest(studyCourse, listener))
    }

    fun loadTodaysLessons(todaysLessonsListener: TodaysLessonsListener) {
        val today = WeekDayTime()

        //Here we have to load all the lesson scheduled for today.
        val dayInterval = today.containingInterval
        loadAndCacheNotCachedLessons(dayInterval, object : WaitForDownloadLessonListener() {
            override fun onFinish(loadingSuccessful: Boolean) {
                if (loadingSuccessful) {
                    cacher.getTodaysCachedLessons(todaysLessonsListener)
                } else {
                    todaysLessonsListener.onLessonsCouldNotBeLoaded()
                }
            }
        })
    }

    fun removeExtraCoursesWithLessonType(lessonTypeId: Int) {
        cacher.removeExtraCoursesWithLessonType(lessonTypeId)
    }

    fun sendExtraCourseLoadingRequest(
            interval: WeekInterval,
            course: ExtraCourse,
            listener: LessonsLoadingListener) {
        sender.processRequest(generateExtraLessonRequest(interval, course, listener))
    }

    fun sendExtraCourseLoadingRequestOneTime(
            interval: WeekInterval,
            extraCourse: ExtraCourse,
            listener: LessonsLoadingListener) {
        val request = generateExtraLessonRequest(interval, extraCourse, listener)
        request.setCacheCheckEnabled(false)
        request.setRetrialsEnabled(false)

        sender.processRequest(request)
    }

    private fun generateExtraLessonRequest(
            interval: WeekInterval,
            extraCourse: ExtraCourse,
            listener: LessonsLoadingListener
    ): ExtraLessonsRequest = ExtraLessonsRequest(interval, extraCourse, listener)

    fun sendStudyCourseLoadingRequest(interval: WeekInterval, listener: LessonsLoadingListener) {
        sender.processRequest(generateStudyCourseLessonRequest(interval, listener))
    }

    fun sendStudyCourseLoadingRequestOneTime(
            interval: WeekInterval,
            listener: LessonsLoadingListener) {

        val request = generateStudyCourseLessonRequest(interval, listener)
        request.areRetrialsEnabled  = true
        request.isCacheCheckEnabled = true

        sender.processRequest(request)
    }

    private fun generateStudyCourseLessonRequest(
            interval: WeekInterval,
            listener: LessonsLoadingListener): StudyCourseLessonsRequest {
        return StudyCourseLessonsRequest(interval, AppPreferences.studyCourse, listener)
    }

    fun diffExtraCourseLessons(
            interval: WeekInterval, course: ExtraCourse,
            cachedLessons: ArrayList<LessonSchedule>, listener: LessonsDifferenceListener) {

        sender.processRequest(ExtraCourseLessonsDiffRequest(interval, course, cachedLessons, listener))
    }

    fun obliterateAllLessonsCache() {
        cacher.obliterateAllLessonsCache()
    }

    fun purgeStudyCourseCache() {
        cacher.purgeStudyCourseCache()
    }

    fun diffStudyCourseLessons(
            interval: WeekInterval, course: StudyCourse,
            lessons: ArrayList<LessonSchedule>, listener: LessonsDifferenceListener) {
        sender.processRequest(StudyCourseLessonsDiffRequest(interval, course, lessons, listener))
    }

    //////////////////////////////////////
    // Requests
    /////////////////////////////////////
    abstract inner class BasicLessonsRequest : IRequest {

        val operationId: Int = PROGRESSIVE_OPERATION_ID_COUNTER++

        override val url: String
            get() = buildRequestURL(courseId, year, calendarIntervalToLoad)

        protected abstract val calendarIntervalToLoad: CalendarInterval
        protected abstract val courseId: Long
        protected abstract val year: Int

        @Throws(JSONException::class)
        protected fun parseResponse(response: String): LessonsSet {
            val jsonResponse = JSONObject(response)

            val lessonsSet = LessonsSet()
            val activitiesJson = jsonResponse.getJSONArray("Attivita")
            parseLessonTypes(lessonsSet, activitiesJson)

            val lessonsJson = jsonResponse.getJSONArray("Eventi")
            lessonsSet.addLessonSchedules(createLessonSchedulesFromJSON(lessonsJson))
            return lessonsSet
        }

        @Throws(JSONException::class)
        private fun parseLessonTypes(lessonsSet: LessonsSet, activitiesJson: JSONArray) {
            for (i in 0 until activitiesJson.length()) {
                lessonsSet.addLessonType(LessonType.fromJson(activitiesJson.getJSONObject(i)))
            }
        }

        @Throws(JSONException::class)
        private fun createLessonSchedulesFromJSON(eventsJson: JSONArray): ArrayList<LessonSchedule> {
            val schedules = ArrayList<LessonSchedule>()
            for (i in 0 until eventsJson.length()) {
                schedules.add(LessonSchedule.fromJson(eventsJson.getJSONObject(i)))
            }
            return schedules
        }

        override //We have nothing to send
        val formToSend: FormBody?
            get() = null
    }

    private inner class FetchRoomForLessonRequest(
            private val lesson: LessonSchedule,
            private val cay: CourseAndYear,
            private val listener: LessonWithRoomFetchedListener) : BasicLessonsRequest() {

        override fun notifyFailure(e: Exception, sender: RequestSender) {
            //In this request we just don't manage errors
            listener.onError(lesson)
        }

        override fun manageResponse(responseToManage: String, sender: RequestSender) {
            try {
                val lessonsSet = parseResponse(responseToManage)
                for (fetchedLesson in lessonsSet.scheduledLessons.values) {
                    if (fetchedLesson.id == lesson.id) {
                        lesson.room = fetchedLesson.room
                        break
                    }
                }

                //Note: here might happen that the lesson we were trying to fetch the room for is not
                //available; actually this happens were rarely, when we try to get the lesson's room
                //but exactly at that time the lessons gets removed. In this case we just keep the
                //lesson as it is, even though the best way to handle this would be to not consider that
                //lesson for further elaborations.
                listener.onUpdateSuccessful(lesson)
            } catch (e: JSONException) {
                notifyFailure(e, sender)
            }

        }

        override fun notifyResponseUnsuccessful(code: Int, sender: RequestSender) {
            listener.onError(lesson)
        }

        override fun notifyOnBeforeSend() {}

        override //For some reasons, trying to fetch too small intervals does not returns us any result!
        val calendarIntervalToLoad: CalendarInterval
            get() = lesson.toExpandedCalendarInterval(Calendar.HOUR_OF_DAY, 4)

        override val courseId: Long
            get() = cay.courseId

        override val year: Int
            get() = cay.year

    }

    open inner class StudyCourseLessonsRequest(
            protected val interval: WeekInterval,
            protected val course: StudyCourse,
            protected var listener: LessonsLoadingListener) : BasicLessonsRequest() {

        var areRetrialsEnabled  = true
        var isCacheCheckEnabled = true

        protected var isRetrying = false

        override fun notifyFailure(exception: Exception, sender: RequestSender) {
            //We had an error trying to load the lessons from the network. We may still
            //have some old cache to try to reuse. In case we do not have such cache, we
            //dispatch the error and keep retrying loading
            if (isRetrying) {
                listener.onErrorHappened(exception, operationId)
                resendRequestIfNeeded(sender)
            } else if (isCacheCheckEnabled) {
                val extraCourses = AppPreferences.extraCourses
                cacher.getLessonsInFreshOrDeadCacheAsync(interval, extraCourses, true) { cache ->
                    if (cache.isIntervalPartiallyOrFullyCached(interval)) {
                        if (cache.hasMissingIntervals()) {
                            //We found only some pieces. We still return these. To prevent the
                            //request from fetching same events multiple time or making it merge
                            //with maybe deleted events we will make the networker load only the
                            //missing pieces
                            listener.onPartiallyCachedResultsFetched(cache)

                            for (notCachedInterval in cache.missingIntervals) {
                                notCachedInterval.launchLoading(this@LessonsController, listener)
                            }

                            listener.onLoadingDelegated(operationId)
                        } else {
                            //We found everything we needed in the old cache
                            listener.onLessonsLoaded(cache, interval, 0)
                        }

                    } else {
                        //Nothing found in cache: we keep retrying loading
                        listener.onErrorHappened(exception, operationId)
                        resendRequestIfNeeded(sender)
                    }
                }
            } else {
                //Cache check disabled: we just retry to fetch
                listener.onErrorHappened(exception, operationId)
                resendRequestIfNeeded(sender)
            }
        }

        override fun manageResponse(response: String, sender: RequestSender) {
            try {
                val lessonsSet = parseResponse(response)

                //Technically we should always be fetching the latest lesson types. In some cases, however
                //we can scroll back so much to be able to see the previous semesters' courses. We do not
                //want to cache courses that are not actual.
                lessonsSet.removeLessonTypesNotInCurrentSemester()

                cacher.cacheLessonsSet(lessonsSet, interval)

                onLessonsSetAvailable(lessonsSet)

                listener.onLessonsLoaded(lessonsSet, interval, operationId)
            } catch (e: Exception) {
                e.printStackTrace()
                BugLogger.logBug("Parsing study course request", e)

                listener.onParsingErrorHappened(e, operationId)

                resendRequestIfNeeded(sender)
            }

        }

        protected open fun onLessonsSetAvailable(lessonsSet: LessonsSet) {
            //Hook methods for elaborations
        }

        internal fun resendRequestIfNeeded(sender: RequestSender) {
            if (areRetrialsEnabled) {
                isRetrying = true
                sender.processRequestAfterTimeout(this)
            } else {
                listener.onLoadingAborted(operationId)
            }
        }

        override fun notifyResponseUnsuccessful(code: Int, sender: RequestSender) {
            listener.onErrorHappened(ResponseUnsuccessfulException(code), operationId)

            //In case of error, we resend the request after the timeout
            resendRequestIfNeeded(sender)
        }

        override fun notifyOnBeforeSend() {
            listener.onLoadingAboutToStart(LessonsLoadingMessage(operationId, interval, isRetrying))
        }

        override val calendarIntervalToLoad: CalendarInterval
            get() = interval.toCalendarInterval()

        override val courseId: Long
            get() = course.courseId

        override val year: Int
            get() = course.year
    }

    private inner class StudyCourseLessonsDiffRequest(
            interval: WeekInterval,
            course: StudyCourse,
            private val cachedLessons: ArrayList<LessonSchedule>,
            private val differenceListener: LessonsDifferenceListener) : StudyCourseLessonsRequest(interval, course, DiffCompletedListener(differenceListener)) {

        init {
            areRetrialsEnabled  = false
            isCacheCheckEnabled = false
        }

        override fun onLessonsSetAvailable(lessonsSet: LessonsSet) {
            val fetchedLessons = ArrayList(lessonsSet.scheduledLessons.values)

            //Do not compare what we filtered
            LessonSchedule.filterLessons(fetchedLessons)
            LessonSchedule.filterLessons(cachedLessons)

            differenceListener.onDiffResult(LessonSchedule.diffLessons(cachedLessons, fetchedLessons))
        }

    }

    private inner class ExtraCourseLessonsDiffRequest(
            interval: WeekInterval,
            extraCourse: ExtraCourse,
            private val cachedLessons: ArrayList<LessonSchedule>,
            private val differenceListener: LessonsDifferenceListener) : ExtraLessonsRequest(interval, extraCourse, DiffCompletedListener(differenceListener)) {

        init {

            setCacheCheckEnabled(false)
            setRetrialsEnabled(false)

        }

        override fun onLessonsSetAvailable(lessonsSet: LessonsSet) {
            val fetchedLessons = ArrayList(lessonsSet.scheduledLessons.values)

            //Do not compare what we filtered
            LessonSchedule.filterLessons(fetchedLessons)
            LessonSchedule.filterLessons(cachedLessons)

            differenceListener.onDiffResult(LessonSchedule.diffLessons(cachedLessons, fetchedLessons))
        }

    }

    open inner class ExtraLessonsRequest(
            val intervalToLoad: WeekInterval,
            val extraCourse: ExtraCourse,
            protected var listener: LessonsLoadingListener) : BasicLessonsRequest() {

        var isRetrying: Boolean = false
            private set
        private var cacheCheckEnabled: Boolean = false
        private var retrialsEnabled: Boolean = false

        init {
            this.isRetrying = false
        }

        override fun notifyFailure(e: Exception, sender: RequestSender) {
            listener.onErrorHappened(e, operationId)

            //Remember to manage cacheCheckEnabled here when retrieving data from dead cache
            resendRequestIfNeeded(sender)
        }

        override fun manageResponse(response: String, sender: RequestSender) {
            try {
                val lessonsSet = parseResponse(response)
                lessonsSet.prepareForExtraCourse(extraCourse)

                onLessonsSetAvailable(lessonsSet)

                cacher.cacheExtraLessonsSet(lessonsSet, intervalToLoad, extraCourse)

                listener.onLessonsLoaded(lessonsSet, intervalToLoad, operationId)
            } catch (e: Exception) {
                e.printStackTrace()
                BugLogger.logBug("Parsing extra lessons request", e)

                listener.onParsingErrorHappened(e, operationId)

                resendRequestIfNeeded(sender)
            }

        }

        protected open fun onLessonsSetAvailable(lessonsSet: LessonsSet) {
            //hook method for further computations
        }

        private fun resendRequestIfNeeded(sender: RequestSender) {
            if (retrialsEnabled) {
                isRetrying = true
                sender.processRequestAfterTimeout(this)
            } else {
                listener.onLoadingAborted(operationId)
            }
        }

        override fun notifyResponseUnsuccessful(code: Int, sender: RequestSender) {
            listener.onErrorHappened(ResponseUnsuccessfulException(code), operationId)

            //In case of error, we resend the request after the timeout
            resendRequestIfNeeded(sender)
        }

        override fun notifyOnBeforeSend() {
            listener.onLoadingAboutToStart(ExtraCoursesLoadingMessage(this))
        }

        override val calendarIntervalToLoad: CalendarInterval
            get() = intervalToLoad.toCalendarInterval()

        override val courseId: Long
            get() = extraCourse.courseId

        override val year: Int
            get() = extraCourse.year

        fun setCacheCheckEnabled(cacheCheckEnabled: Boolean) {
            this.cacheCheckEnabled = cacheCheckEnabled
        }

        fun setRetrialsEnabled(retrialsEnabled: Boolean) {
            this.retrialsEnabled = retrialsEnabled
        }
    }

    inner class ListLessonsRequest(
            private val studyCourse: StudyCourse,
            private val listener: ListLessonsListener) : BasicLessonsRequest() {
        private val weekInterval: WeekInterval

        init {
            this.weekInterval = WeekInterval(-2, +2)
        }

        override fun notifyFailure(e: Exception, sender: RequestSender) {
            listener.onErrorHappened(e)

        }

        override fun manageResponse(response: String, sender: RequestSender) {
            try {
                val lessonsSet = parseResponse(response)
                listener.onLessonTypesRetrieved(lessonsSet.lessonTypes.values)
            } catch (e: Exception) {
                e.printStackTrace()
                BugLogger.logBug("Parsing lessons list request", e)

                listener.onParsingErrorHappened(e)
            }

        }

        override fun notifyResponseUnsuccessful(code: Int, sender: RequestSender) {
            listener.onErrorHappened(ResponseUnsuccessfulException(code))

        }

        override fun notifyOnBeforeSend() {
            //Nothing to do
        }

        override val calendarIntervalToLoad: CalendarInterval
            get() = weekInterval.toCalendarInterval()

        override val courseId: Long
            get() = studyCourse.courseId

        override val year: Int
            get() = studyCourse.year

    }

    companion object {

        private var PROGRESSIVE_OPERATION_ID_COUNTER = 1

        private fun buildRequestURL(
                courseId: Long,
                year: Int,
                intervalToLoad: CalendarInterval): String {
            return if (Config.DEBUG_MODE && Config.LAUNCH_REQUESTS_TO_DEBUG_SERVER) {
                Config.DEBUG_SERVER_URL
            } else String.format(
                    Locale.CANADA,
                    "http://webapps.unitn.it/Orari/it/Web/AjaxEventi/c/%d-%d/agendaWeek?_=%d&start=%d&end=%d",
                    courseId,
                    year,
                    System.currentTimeMillis(),
                    intervalToLoad.from.timeInMillis / 1000,
                    intervalToLoad.to.timeInMillis / 1000
            )

        }
    }
}
