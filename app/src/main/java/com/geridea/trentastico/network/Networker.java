package com.geridea.trentastico.network;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.LessonsSetAvailableListener;
import com.geridea.trentastico.database.NotCachedIntervalsListener;
import com.geridea.trentastico.database.TodaysLessonsListener;
import com.geridea.trentastico.model.CourseAndYear;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedInterval;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.request.FetchRoomForLessonRequest;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.ListLessonsRequest;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.network.request.SendFeedbackRequest;
import com.geridea.trentastico.network.request.listener.FeedbackSendListener;
import com.geridea.trentastico.network.request.listener.LessonsDifferenceListener;
import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.request.listener.LessonsWithRoomListener;
import com.geridea.trentastico.network.request.listener.LessonsWithRoomMultipleListener;
import com.geridea.trentastico.network.request.listener.ListLessonsListener;
import com.geridea.trentastico.network.request.listener.WaitForDownloadLessonListener;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekDayTime;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class Networker {

    private static RequestSender requestSender = new RequestSender();

    /**
     * Loads the lessons in the given period. Fetches all the possible lesson from the fresh cache
     * or dead cache and the remaining from internet.
     */
    public static void loadLessons(
            final WeekInterval intervalToLoad, final LessonsLoadingListener listener,
            final LoadingIntervalKnownListener intervalListener) {

        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        Cacher.getLessonsInFreshOrDeadCacheAsync(intervalToLoad, extraCourses, false, new LessonsSetAvailableListener() {
            @Override
            public void onLessonsSetAvailable(CachedLessonsSet cacheSet) {
                if (cacheSet.hasMissingIntervals()) {
                    if(cacheSet.wereSomeLessonsFoundInCache()){
                        listener.onPartiallyCachedResultsFetched(cacheSet);
                    } else {
                        listener.onNothingFoundInCache();
                    }

                    for (NotCachedInterval interval: cacheSet.getMissingIntervals()) {
                        loadInterval(interval, listener);
                    }
                } else {
                    //We found everything we needed in cache
                    listener.onLessonsLoaded(cacheSet, intervalToLoad, 0);
                }

                intervalListener.onIntervalsToLoadKnown(intervalToLoad, cacheSet.getMissingIntervals());
            }
        });
    }

    public static void diffLessonsInCache(final WeekInterval intervalToCheck, final LessonsDifferenceListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        Cacher.getLessonsInFreshOrDeadCacheAsync(intervalToCheck, extraCourses, false, new LessonsSetAvailableListener() {
            @Override
            public void onLessonsSetAvailable(CachedLessonsSet cacheSet) {
                if(cacheSet.isIntervalPartiallyOrFullyCached(intervalToCheck)){
                    ArrayList<CachedInterval> cachedIntervals = cacheSet.getCachedIntervals();

                    listener.onNumberOfRequestToSendKnown(cachedIntervals.size());

                    for (CachedInterval cachedInterval: cachedIntervals) {
                        processRequest(cachedInterval.generateDiffRequest(listener));
                    }
                } else {
                    listener.onNoLessonsInCache();
                }
            }
        });
    }

    public static void loadAndCacheNotCachedLessons(WeekInterval interval, final WaitForDownloadLessonListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        Cacher.getNotCachedSubintervals(interval, extraCourses, new NotCachedIntervalsListener(){

            public void onIntervalsKnown(ArrayList<NotCachedInterval> notCachedIntervals ){
                if (notCachedIntervals.isEmpty()) {
                    listener.onNothingToLoad();
                } else {
                    for (NotCachedInterval notCachedInterval : notCachedIntervals) {
                        processRequest(notCachedInterval.generateOneTimeRequest(listener));
                    }
                }
            }
        });
    }

    private static void loadInterval(NotCachedInterval interval, LessonsLoadingListener listener) {
        processRequest(interval.generateRequest(listener));
    }

    private static void processRequest(IRequest requestToSend) {
        requestSender.processRequest(requestToSend);
    }

    public static void loadCoursesOfStudyCourse(StudyCourse studyCourse, ListLessonsListener listener) {
        processRequest(new ListLessonsRequest(studyCourse, listener));
    }

    public static void loadTodaysLessons(final TodaysLessonsListener todaysLessonsListener) {
        final WeekDayTime today = new WeekDayTime();

        //Here we have to load all the lesson scheduled for today.
        WeekInterval dayInterval = today.getContainingInterval();
        loadAndCacheNotCachedLessons(dayInterval, new WaitForDownloadLessonListener() {
            @Override
            public void onFinish(boolean loadingSuccessful) {
                if (loadingSuccessful) {
                    Cacher.getTodaysLessons(todaysLessonsListener);
                } else {
                    todaysLessonsListener.onLessonsCouldNotBeLoaded();
                }
            }
        });
    }

    public static void loadRoomsForLessonsIfMissing(ArrayList<LessonSchedule> lessons, final LessonsWithRoomListener listener) {
        ArrayList<LessonSchedule> lessonsToLoad = new ArrayList<>();
        for (LessonSchedule lesson : lessons) {
            if(!lesson.hasRoomSpecified()){
                lessonsToLoad.add(lesson);
            }
        }

        if (lessonsToLoad.isEmpty()) {
            listener.onLoadingCompleted(lessons);
        } else {
            LessonsWithRoomMultipleListener fetchRoomListener
                    = new LessonsWithRoomMultipleListener(lessonsToLoad, listener);

            for (LessonSchedule lesson: lessonsToLoad) {
                CourseAndYear cay = LessonSchedule.findCourseAndYearForLesson(lesson);
                processRequest(new FetchRoomForLessonRequest(lesson, cay, fetchRoomListener));
            }

        }
    }

    public static void sendFeedback(String feedback, String name, String email, FeedbackSendListener listener) {
        processRequest(new SendFeedbackRequest(feedback, name, email, listener));
    }


}
