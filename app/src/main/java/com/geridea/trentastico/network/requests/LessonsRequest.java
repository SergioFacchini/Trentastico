package com.geridea.trentastico.network.requests;

import com.android.volley.VolleyError;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Signal1;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public class LessonsRequest extends AbstractServerRequest {

    private final StudyCourse studyCourse;
    private LessonsLoadingListener listener;

    private final WeekInterval intervalToLoad;

    /**
     * Dispatched when the request has been successfully fulfilled.
     */
    public final Signal1<LessonsSet> onRequestSuccessful = new Signal1<>();

    /**
     * Dispatched when the request has encountered an error while trying to parse the response.
     */
    public final Signal1<Exception> onParsingErrorHappened = new Signal1<>();
    /**
     * Dispatched when there is an error while trying to get lessons from internet.
     */
    public final Signal1<VolleyError> onNetworkErrorHappened = new Signal1<>();


    public LessonsRequest(final WeekInterval intervalToLoad, StudyCourse studyCourse, LessonsLoadingListener listener) {
        this.intervalToLoad = intervalToLoad;
        this.studyCourse = studyCourse;
        this.listener = listener;
    }

    @Override
    public void deliverError(VolleyError error) {
        listener.onErrorHappened(error, getOperationId());
        onNetworkErrorHappened.dispatch(error);
    }

    @Override
    public StudyCourse getStudyCourse() {
        return studyCourse;
    }

    @Override
    public WeekInterval getIntervalToLoad() {
        return intervalToLoad;
    }

    @Override
    public void onResponse(String response) {
        try {
            LessonsSet lessonsSet = parseResponse(response);

            //Technically we should always be fetching the latest lesson types. In some cases, however
            //we can scroll back so much to be able to see the previous semesters' courses. We do not
            //want to cache courses that are not actual.
            lessonsSet.removeLessonTypesNotInCurrentSemester();

            Cacher.cacheLessonsSet(lessonsSet, getIntervalToLoad());

            getListener().onLessonsLoaded(lessonsSet, getIntervalToLoad(), getOperationId());

            onRequestSuccessful.dispatch(lessonsSet);
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e, getOperationId());
            onParsingErrorHappened.dispatch(e);
        }
    }

    public LessonsLoadingListener getListener() {
        return listener;
    }
}
