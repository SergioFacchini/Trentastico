package com.geridea.trentastico.network;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.support.annotation.Nullable;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedInterval;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.ListLessonsRequest;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.network.request.listener.LessonsDifferenceListener;
import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.request.listener.ListLessonsListener;
import com.geridea.trentastico.network.request.listener.WaitForDownloadLessonListener;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class Networker {

    private static RequestSender requestSender = new RequestSender();

    /**
     * Loads the lessons in the given period. Fetches all the possible lesson from the fresh cache
     * or dead cache and the remaining from internet.
     * @return a list of intervals that will be fetched from the network
     */
    @Nullable
    public static ArrayList<NotCachedInterval> loadLessons(WeekInterval intervalToLoad, LessonsLoadingListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        CachedLessonsSet cacheSet = Cacher.getLessonsInFreshOrDeadCache(intervalToLoad, extraCourses, false);
        if (cacheSet.hasMissingIntervals()) {
            if(cacheSet.wereSomeLessonsFoundInCache()){
                listener.onPartiallyCachedResultsFetched(cacheSet);
            }

            for (NotCachedInterval interval: cacheSet.getMissingIntervals()) {
                loadInterval(interval, listener);
            }
        } else {
            //We found everything we needed in cache
            listener.onLessonsLoaded(cacheSet, intervalToLoad, 0);
        }

        return cacheSet.getMissingIntervals();
    }

    public static void diffLessonsInCache(WeekInterval intervalToCheck, LessonsDifferenceListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        CachedLessonsSet cacheSet = Cacher.getLessonsInFreshOrDeadCache(intervalToCheck, extraCourses, false);

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

    public static void loadAndCacheNotCachedLessons(WeekInterval interval, WaitForDownloadLessonListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        ArrayList<NotCachedInterval> notCachedIntervals = Cacher.getNotCachedSubintervals(interval, extraCourses);
        for (NotCachedInterval notCachedInterval : notCachedIntervals) {
            processRequest(notCachedInterval.generateOneTimeRequest(listener));
        }

        if (notCachedIntervals.isEmpty()) {
            listener.onNothingToLoad();
        }

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

}
