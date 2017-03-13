package trentastico.geridea.com.trentastico.network;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.threerings.signals.Listener1;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import trentastico.geridea.com.trentastico.Config;
import trentastico.geridea.com.trentastico.database.Cacher;
import trentastico.geridea.com.trentastico.model.LessonsSet;
import trentastico.geridea.com.trentastico.model.StudyCourse;
import trentastico.geridea.com.trentastico.model.cache.CachedLessonsSet;

public class Networker {

    private static Context CONTEXT;

    private static RequestQueuer queuer = new RequestQueuer();

    public static void init(Context context) {
        CONTEXT = context;
    }

    public static void loadLessonsOfCourse(
            Calendar fromWhen, Calendar toWhen, StudyCourse studyCourse, LessonsFetchedListener listener) {

        CachedLessonsSet lessons = Cacher.getLessonsInCacheIfAvailable(fromWhen, toWhen, studyCourse);
        if (lessons != null) {
            listener.onLessonsLoaded(lessons, fromWhen, toWhen);
        } else {
            performLoadingRequest(fromWhen, toWhen, studyCourse, listener);
        }
    }

    private static void performLoadingRequest(
            Calendar fromWhen, Calendar toWhen, StudyCourse studyCourse, LessonsFetchedListener listener) {

        if (!queuer.isWorking()) {
            queuer = new RequestQueuer();
        }

        queuer.enqueueRequest(new LessonsRequest(fromWhen, toWhen, studyCourse, listener));
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
