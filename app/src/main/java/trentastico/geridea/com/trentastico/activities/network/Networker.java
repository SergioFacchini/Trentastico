package trentastico.geridea.com.trentastico.activities.network;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.toolbox.Volley;
import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;

import java.util.Calendar;
import java.util.concurrent.ConcurrentLinkedQueue;

import trentastico.geridea.com.trentastico.activities.model.LessonsSet;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;

public class Networker {

    private static Context CONTEXT;

    private static RequestQueuer queuer = new RequestQueuer();

    public static void init(Context context) {
        CONTEXT = context;
    }

    public static void loadLessonsOfCourse(
            Calendar fromWhen, Calendar toWhen, StudyCourse studyCourse, LessonsFetchedListener listener) {

        LessonsSet lessons = Cacher.getLessonsInCacheIfAvailable(fromWhen, toWhen, studyCourse);
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

        private boolean isWorking = false;

        /**
         * Queue of all the pending requests that have to be made
         */
        private static ConcurrentLinkedQueue<LessonsRequest> workingQueue = new ConcurrentLinkedQueue<>();

        @Override
        protected synchronized Void doInBackground(Void... lessonsRequests) {
            isWorking = true;

            if (!workingQueue.isEmpty()) {
                final LessonsRequest request = workingQueue.poll();
                request.onRequestTerminated.connect(new Listener0() {
                    @Override
                    public void apply() {
                        doInBackground();
                    }
                });

                request.inRequestAboutToBeSent.dispatch();
                request.onRequestSuccessful.connect(new Listener1<LessonsSet>() {
                    @Override
                    public void apply(LessonsSet result) {
                        Cacher.cacheLessonsSet(request, result);
                    }
                });

                Volley.newRequestQueue(CONTEXT).add(request);
            } else {
                isWorking = false;
            }

            return null;
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
