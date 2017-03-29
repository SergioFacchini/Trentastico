package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.support.annotation.Nullable;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.NotCachedInterval;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.request.ListLessonsRequest;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class Networker {

    private static RequestSender requestSender = new RequestSender();

    public static void init(Context context) {
        ;
    }

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

    private static void loadInterval(NotCachedInterval interval, LessonsLoadingListener listener) {
        requestSender.processRequest(interval.generateRequest(listener));
    }

    public static void loadCoursesOfStudyCourse(StudyCourse studyCourse, ListLessonsListener listener) {
        requestSender.processRequest(new ListLessonsRequest(studyCourse, listener));
    }

}
