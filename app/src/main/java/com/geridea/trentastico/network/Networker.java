package com.geridea.trentastico.network;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.geridea.trentastico.Config;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.NotCachedInterval;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.operations.ExtraCoursesLoadingMessage;
import com.geridea.trentastico.network.operations.LessonsLoadingMessage;
import com.geridea.trentastico.network.requests.AbstractServerRequest;
import com.geridea.trentastico.network.requests.ExtraCourseLessonsRequest;
import com.geridea.trentastico.network.requests.LessonsRequest;
import com.geridea.trentastico.network.requests.ListLessonTypesRequest;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import static com.geridea.trentastico.Config.PRE_LOADING_WAITING_TIME_MS;

public class Networker {

    private static Context CONTEXT;

    private static RequestSender queuer = new RequestSender();

    public static void init(Context context) {
        CONTEXT = context;
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
                performLoadingRequest(interval, listener);
            }
        } else {
            //We found everything we needed in cache
            listener.onLessonsLoaded(cacheSet, intervalToLoad, 0);
        }

        return cacheSet.getMissingIntervals();
    }

    private static void performLoadingRequest(NotCachedInterval interval, LessonsLoadingListener listener) {
        queuer.enqueueRequest(interval.generateRequest(listener));
    }

    public static void loadCoursesOfStudyCourse(StudyCourse studyCourse, CoursesOfStudyCourseListener listener) {
        queuer.enqueueRequest(new ListLessonTypesRequest(studyCourse, listener));
    }

    public interface CoursesOfStudyCourseListener {
        void onErrorHappened(VolleyError error);

        void onParsingErrorHappened(Exception e);

        void onLessonTypesRetrieved(Collection<LessonType> lessonTypes);
    }

    private static class RequestSender {

        private Timer timeoutWaiter = new Timer();

        private void processRequest(final ListLessonTypesRequest request, boolean isARetry) {
            if (!isARetry) {
                request.onParsingErrorHappened.connect(new Listener1<Exception>() {
                    @Override
                    public void apply(Exception e) {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });
                request.onNetworkErrorHappened.connect(new Listener0() {
                    @Override
                    public void apply() {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });
            }

            sendRequest(request);
        }

        private void processRequest(final ExtraCourseLessonsRequest request, boolean isARetry) {
            if (!isARetry) {
                request.onParsingErrorHappened.connect(new Listener1<Exception>() {
                    @Override
                    public void apply(Exception e) {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });
                request.onNetworkErrorHappened.connect(new Listener0() {
                    @Override
                    public void apply() {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });

                request.getListener().onLoadingAboutToStart(new ExtraCoursesLoadingMessage(
                        request.getOperationId(), request.getIntervalToLoad(), request.getExtraCourse())
                );
            }

            sendRequest(request);
        }

        private Request<String> sendRequest(AbstractServerRequest request) {
            if (Config.DEBUG_MODE && PRE_LOADING_WAITING_TIME_MS != 0) {
                try {
                    Thread.sleep(PRE_LOADING_WAITING_TIME_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return Volley.newRequestQueue(CONTEXT).add(request);
        }

        private void processRequest(EnqueueableOperation operation, boolean isARetry) {
            if (operation instanceof LessonsRequest) {
                processRequest((LessonsRequest) operation, isARetry);
            } else if(operation instanceof ListLessonTypesRequest){
                processRequest((ListLessonTypesRequest) operation, isARetry);
            } else if(operation instanceof ExtraCourseLessonsRequest){
                processRequest((ExtraCourseLessonsRequest) operation, isARetry);
            } else {
                BugLogger.logBug();
                throw new RuntimeException("You forgot to add the appropriate processRequest() to the RequestSender!");
            }
        }

        private void processRequest(final LessonsRequest request, final boolean isARetry) {
            //When recalled, we want to know if we already tried to find the lessons in the old
            //cache
            final boolean[] isARetryCache = new boolean[1];
            isARetryCache[0] = isARetry;

            if (!isARetry) {
                request.onParsingErrorHappened.connect(new Listener1<Exception>() {
                    @Override
                    public void apply(Exception e) {
                        waitForTimeoutAndReprocessRequest(request);
                    }
                });

                request.onNetworkErrorHappened.connect(new Listener1<VolleyError>() {
                    @Override
                    public void apply(VolleyError error) {
                        //We had an error trying to load the lessons from the network. We may still
                        //have some old cache to try to reuse. In case we do not have such cache, we
                        //dispatch the error and keep retrying loading
                        if (isARetryCache[0]) {
                            request.getListener().onErrorHappened(error, request.getOperationId());
                            waitForTimeoutAndReprocessRequest(request);
                        } else {
                            ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
                            CachedLessonsSet cache = Cacher.getLessonsInFreshOrDeadCache(request.getIntervalToLoad(), extraCourses, true);
                            if (cache.wereSomeLessonsFoundInCache()) {
                                if (cache.hasMissingIntervals()) {
                                    //We found only some pieces. We still return these. To prevent the
                                    //request from fetching same events multiple time or making it merge
                                    //with maybe deleted events we will make the networker load only the
                                    //missing pieces
                                    request.getListener().onPartiallyCachedResultsFetched(cache);

                                    for (WeekInterval notCachedInterval : cache.getMissingIntervals()) {
                                        loadLessons(notCachedInterval, request.getListener());
                                    }
                                } else {
                                    //We found everything we needed in the old cache
                                    request.getListener().onLessonsLoaded(cache, request.getIntervalToLoad(), 0);
                                }

                            } else {
                                //Nothing found in cache: we keep retrying loading
                                request.getListener().onErrorHappened(error, request.getOperationId());
                                waitForTimeoutAndReprocessRequest(request);
                            }
                        }
                    }
                });
            }

            request.getListener().onLoadingAboutToStart(
                new LessonsLoadingMessage(request.getOperationId(), request.getIntervalToLoad())
            );
            sendRequest(request);
        }

        private void waitForTimeoutAndReprocessRequest(final EnqueueableOperation request) {
            timeoutWaiter.schedule(new TimerTask() {
                @Override
                public void run() {
                    processRequest(request, true);
                }
            }, Config.WAITING_TIME_AFTER_A_REQUEST_FAILED);
        }

        public void enqueueRequest(EnqueueableOperation operation) {
            processRequest(operation, false);
        }

    }

}
