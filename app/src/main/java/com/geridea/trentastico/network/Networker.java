package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.AppPreferences;
import com.threerings.signals.Listener1;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedLessonsSet;

public class Networker {

    private static Context CONTEXT;

    private static RequestQueuer queuer = new RequestQueuer();

    public static void init(Context context) {
        CONTEXT = context;
    }

    /**
     * Loads the lessons in the given period. Fetches all the possible lesson from the fresh cache
     * or dead cache and the remaining from internet.
     * @return a list of intervals that will be fetched from the network
     */
    @Nullable
    public static ArrayList<WeekInterval> loadLessons(WeekInterval intervalToLoad, LessonsFetchedListener listener) {
        CachedLessonsSet cacheSet = Cacher.getLessonsInFreshOrDeadCache(intervalToLoad);
        if (cacheSet.hasMissingIntervals()) {
            if(cacheSet.wereSomeLessonsFoundInCache()){
                listener.onPartiallyCachedResultsFetched(cacheSet);
            }

            for (WeekInterval interval: cacheSet.getMissingIntervals()) {
                performLoadingRequest(interval, listener);
            }
        } else {
            //We found everything we needed in cache
            listener.onLessonsLoaded(cacheSet, intervalToLoad);
        }

        return cacheSet.getMissingIntervals();
    }

    private static void performLoadingRequest(WeekInterval intervalToLoad, LessonsFetchedListener listener) {
        if (!queuer.isWorking()) {
            queuer = new RequestQueuer();
        }

        StudyCourse studyCourse = AppPreferences.getStudyCourse();
        queuer.enqueueRequest(new LessonsRequest(intervalToLoad, studyCourse, listener));
    }


    private static class RequestQueuer extends AsyncTask<Void, Void, Void> {

        private Timer timeoutWaiter = new Timer();

        private boolean isWorking = false;

        /**
         * Queue of all the pending requests that have to be made
         */
        private static ConcurrentLinkedQueue<LessonsRequest> workingQueue = new ConcurrentLinkedQueue<>();

        @Override
        protected synchronized Void doInBackground(Void... lessonsRequests) {
            isWorking = true;

            if (!workingQueue.isEmpty()) {
                processRequest(workingQueue.poll(), false);
            } else {
                isWorking = false;
            }

            return null;
        }

        private void processRequest(final LessonsRequest request, boolean isARetry) {
            if (!isARetry) {
                request.onRequestSuccessful.connect(new Listener1<LessonsSet>() {
                    @Override
                    public void apply(LessonsSet result) {
                        Cacher.cacheLessonsSet(request, result);

                        //Start managing the next request
                        doInBackground();
                    }
                });

                request.onNetworkErrorHappened.connect(new Listener1<VolleyError>() {
                    @Override
                    public void apply(VolleyError arg1) {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });

                request.onParsingErrorHappened.connect(new Listener1<Exception>() {
                    @Override
                    public void apply(Exception arg1) {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });
            }

            request.inRequestAboutToBeSent.dispatch();
            Volley.newRequestQueue(CONTEXT).add(request);
        }

        private void waitForTimeoutAndReprocessRequest(final LessonsRequest request) {
            timeoutWaiter.schedule(new TimerTask() {
                @Override
                public void run() {
                    processRequest(request, true);
                }
            }, Config.WAITING_TIME_AFTER_A_REQUEST_FAILED);
        }

        public void enqueueRequest(LessonsRequest request) {
            workingQueue.add(request);

            if (!isWorking) {
                this.execute();
            }
        }

        public boolean isWorking() {
            return isWorking;
        }
    }

}
