package com.geridea.trentastico.network;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.TodaysLessonsListener;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.LibraryOpeningTimesController;
import com.geridea.trentastico.network.controllers.SendFeedbackController;
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener;
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener;
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.controllers.listener.LessonsWithRoomListener;
import com.geridea.trentastico.network.controllers.listener.LibraryOpeningTimesListener;
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener;
import com.geridea.trentastico.network.controllers.listener.WaitForDownloadLessonListener;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;
import java.util.Calendar;

public class Networker {

    //Executors
    private static LessonsController lessonsController;
    private static SendFeedbackController sendFeedbackController;
    private static LibraryOpeningTimesController libraryOpeningTimes;


    private Networker() { }

    public static void init(Cacher cacher){
        RequestSender requestSender = new RequestSender();

        sendFeedbackController = new SendFeedbackController(       requestSender, cacher);
        libraryOpeningTimes    = new LibraryOpeningTimesController(requestSender, cacher);
        lessonsController      = new LessonsController(            requestSender, cacher);

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
     *                         to be loaded
     */
    public static void loadLessons(WeekInterval interval,  LessonsLoadingListener listener, LoadingIntervalKnownListener intervalListener) {
        lessonsController.loadLessons(interval, listener, intervalListener);
    }

    public static void diffLessonsInCache(final WeekInterval intervalToCheck, final LessonsDifferenceListener listener) {
        lessonsController.diffLessonsInCache(intervalToCheck, listener);
    }

    public static void loadAndCacheNotCachedLessons(WeekInterval interval, final WaitForDownloadLessonListener listener) {
        lessonsController.loadAndCacheNotCachedLessons(interval, listener);
    }

    public static void loadCoursesOfStudyCourse(StudyCourse studyCourse, ListLessonsListener listener) {
        lessonsController.loadCoursesOfStudyCourse(studyCourse, listener);
    }

    public static void loadTodaysLessons(final TodaysLessonsListener todaysLessonsListener) {
        lessonsController.loadTodaysLessons(todaysLessonsListener);
    }

    public static void loadRoomsForLessonsIfMissing(ArrayList<LessonSchedule> lessons, final LessonsWithRoomListener listener) {
        lessonsController.loadRoomsForLessonsIfMissing(lessons, listener);
    }

    public static void getTodaysCachedLessons(TodaysLessonsListener todaysLessonsListener) {
        lessonsController.getTodaysCachedLessons(todaysLessonsListener);
    }

    /**
     * Removes all the extra courses and lessons having the given lesson type
     */
    public static void removeExtraCoursesWithLessonType(int lessonTypeId) {
        lessonsController.removeExtraCoursesWithLessonType(lessonTypeId);
    }

    /**
     * Deletes all the cache about lessons and lesson types.
     */
    public static void obliterateLessonsCache() {
        lessonsController.obliterateAllLessonsCache();
    }

    /**
     * Deletes all the cache about the currently chosen study course
     */
    public static void purgeStudyCourseCache() {
        lessonsController.purgeStudyCourseCache();
    }

    //----------------------------
    // Feedback
    //----------------------------
    public static void sendFeedback(String feedback, String name, String email, FeedbackSendListener listener) {
        sendFeedbackController.sendFeedback(feedback, name, email, listener);
    }

    //----------------------------
    // Library opening times
    //----------------------------
    public static void getLibraryOpeningTimes(final Calendar day, final LibraryOpeningTimesListener listener) {
        libraryOpeningTimes.getLibraryOpeningTimes(day, listener);
    }

}
