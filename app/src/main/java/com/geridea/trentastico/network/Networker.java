package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.geridea.trentastico.utils.time.CalendarInterval;
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
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal2;

public class Networker {

    private static Context CONTEXT;

    private static RequestQueuer queuer = new RequestQueuer();

    public static void init(Context context) {
        CONTEXT = context;
    }

    public static final Signal1<CalendarInterval> onLoadingAboutToStart            = new Signal1<>();
    public static final Signal2<LessonsSet, WeekInterval> onLessonsLoaded          = new Signal2<>();
    public static final Signal1<VolleyError> onErrorHappened                       = new Signal1<>();
    public static final Signal1<Exception> onParsingErrorHappened                  = new Signal1<>();
    public static final Signal1<CachedLessonsSet> onPartiallyCachedResultsFetched  = new Signal1<>();

    /**
     * Loads the lessons in the given period. Fetches all the possible lesson from the fresh cache
     * or dead cache and the remaining from internet.
     * @return a list of intervals that will be fetched from the network
     */
    @Nullable
    public static ArrayList<WeekInterval> loadLessons(WeekInterval intervalToLoad) {
        CachedLessonsSet cacheSet = Cacher.getLessonsInFreshOrDeadCache(intervalToLoad, false);
        if (cacheSet.hasMissingIntervals()) {
            if(cacheSet.wereSomeLessonsFoundInCache()){
                onPartiallyCachedResultsFetched.dispatch(cacheSet);
            }

            for (WeekInterval interval: cacheSet.getMissingIntervals()) {
                performLoadingRequest(interval);
            }
        } else {
            //We found everything we needed in cache
            onLessonsLoaded.dispatch(cacheSet, intervalToLoad);
        }

        return cacheSet.getMissingIntervals();
    }

    private static void performLoadingRequest(WeekInterval intervalToLoad) {
        if (!queuer.isWorking()) {
            queuer = new RequestQueuer();
        }

        StudyCourse studyCourse = AppPreferences.getStudyCourse();
        queuer.enqueueRequest(new LessonsRequest(intervalToLoad, studyCourse));
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

        private void processRequest(final LessonsRequest request, final boolean isARetry) {
            //When recalled, we want to know if we already tried to find the lessons in the old
            //cache
            final boolean[] isARetryCache = new boolean[1];
            isARetryCache[0] = isARetry;

            if (!isARetry) {
                request.onRequestSuccessful.connect(new Listener1<LessonsSet>() {
                    @Override
                    public void apply(LessonsSet result) {
                        Cacher.cacheLessonsSet(result, request.getIntervalToLoad());

                        onLessonsLoaded.dispatch(result, request.getIntervalToLoad());

                        //Start managing the next request
                        doInBackground();
                    }
                });

                request.onNetworkErrorHappened.connect(new Listener1<VolleyError>() {
                    @Override
                    public void apply(VolleyError error) {
                        //We had an error trying to load the lessons from the network. We may still
                        //have some old cache to try to reuse. In case we do not have such cache, we
                        //dispatch the error and keep retrying loading
                        if (isARetryCache[0]) {
                            onErrorHappened.dispatch(error);
                            waitForTimeoutAndReprocessRequest(request);
                        } else {
                            CachedLessonsSet cache = Cacher.getLessonsInFreshOrDeadCache(request.getIntervalToLoad(), true);
                            if (cache.wereSomeLessonsFoundInCache()) {
                                if (cache.hasMissingIntervals()) {
                                    //We found only some pieces. We still return these. To prevent the
                                    //request from fetching same events multiple time or making it merge
                                    //with maybe deleted events we will make the networker load only the
                                    //missing pieces
                                    onPartiallyCachedResultsFetched.dispatch(cache);

                                    for (WeekInterval notCachedInterval : cache.getMissingIntervals()) {
                                        loadLessons(notCachedInterval);
                                    }
                                } else {
                                    //We found everything we needed in the old cache
                                    onLessonsLoaded.dispatch(cache, request.getIntervalToLoad());
                                }

                                //Start managing the next request
                                doInBackground();
                            } else {
                                //Nothing found in cache: we keep retrying loading
                                onErrorHappened.dispatch(error);
                                waitForTimeoutAndReprocessRequest(request);
                            }
                        }
                    }
                });

                request.onParsingErrorHappened.connect(new Listener1<Exception>() {
                    @Override
                    public void apply(Exception e) {
                        onParsingErrorHappened.dispatch(e);

                        waitForTimeoutAndReprocessRequest(request);
                    }
                });
            }

            onLoadingAboutToStart.dispatch(request.getIntervalToLoad().toCalendarInterval());
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
