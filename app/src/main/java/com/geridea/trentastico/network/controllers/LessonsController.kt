package com.geridea.trentastico.network.controllers


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.LoadingIntervalKnownListener
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener
import com.geridea.trentastico.network.controllers.listener.WaitForDownloadLessonListener
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.utils.time.WeekInterval

class LessonsController(sender: RequestSender, cacher: Cacher) : BasicController(sender, cacher) {

    fun getTodaysCachedLessons(todaysLessonsListener: TodaysLessonsListener) = cacher.getTodaysCachedLessons(todaysLessonsListener)

    fun loadLessons(
            intervalToLoad: WeekInterval, listener: LessonsLoadingListener,
            intervalListener: LoadingIntervalKnownListener) {

        //TODO: implement after courses loading
    }

    fun diffLessonsInCache(intervalToCheck: WeekInterval, listener: LessonsDifferenceListener) {
        //TODO: implement after courses loading
    }

    fun loadAndCacheNotCachedLessons(
            interval: WeekInterval,
            listener: WaitForDownloadLessonListener) {

        //TODO: implement after courses loading
    }

    fun loadCoursesOfStudyCourse(
            studyCourse: StudyCourse,
            listener: ListLessonsListener) = {
        //TODO: implement after courses loading

    }

    fun loadTodaysLessons(todaysLessonsListener: TodaysLessonsListener) {
        //TODO: implement after courses loading
    }

    fun removeExtraCoursesWithLessonType(lessonTypeId: String) = cacher.removeExtraCoursesWithLessonType(lessonTypeId)

    fun sendExtraCourseLoadingRequest(
            interval: WeekInterval,
            course: ExtraCourse,
            listener: LessonsLoadingListener) {

        //TODO: implement after courses loading

    }

    fun sendExtraCourseLoadingRequestOneTime(
            interval: WeekInterval,
            extraCourse: ExtraCourse,
            listener: LessonsLoadingListener) {

        //TODO: implement after courses loading

    }

    fun sendStudyCourseLoadingRequest(
            interval: WeekInterval,
            listener: LessonsLoadingListener) {
        //TODO: implement after courses loading
    }

    fun sendStudyCourseLoadingRequestOneTime(
            interval: WeekInterval,
            listener: LessonsLoadingListener) {

        //TODO: implement after courses loading
    }

    fun obliterateAllLessonsCache() = cacher.obliterateAllLessonsCache()

    fun purgeStudyCourseCache() = cacher.purgeStudyCourseCache()

}
