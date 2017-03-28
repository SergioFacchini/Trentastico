package com.geridea.trentastico.network.requests;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.android.volley.VolleyError;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Signal1;

public class ExtraCourseLessonsRequest extends AbstractServerRequest {

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


    private final WeekInterval interval;
    private final LessonsLoadingListener listener;
    private final ExtraCourse extraCourse;

    public ExtraCourseLessonsRequest(WeekInterval interval, LessonsLoadingListener listener, ExtraCourse extraCourse) {
        this.interval = interval;
        this.listener = listener;
        this.extraCourse = extraCourse;
    }

    @Override
    protected void onResponse(String response) {
        try {
            LessonsSet lessonsSet = parseResponse(response);
            lessonsSet.prepareForExtraCourse(extraCourse);

            Cacher.cacheExtraLessonsSet(lessonsSet, getIntervalToLoad(), extraCourse);

            listener.onLessonsLoaded(lessonsSet, getIntervalToLoad(), getOperationId());

            onRequestSuccessful.dispatch(lessonsSet);
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e, getOperationId());
            onParsingErrorHappened.dispatch(e);
        }
    }

    @Override
    public void deliverError(VolleyError error) {
        listener.onErrorHappened(error, getOperationId());
        onNetworkErrorHappened.dispatch(error);
    }

    @Override
    public StudyCourse getStudyCourse() {
        return new StudyCourse(-1, extraCourse.getCourseId(), extraCourse.getYear());
    }

    @Override
    public WeekInterval getIntervalToLoad() {
        return interval;
    }

    public LessonsLoadingListener getListener() {
        return listener;
    }

    public ExtraCourse getExtraCourse() {
        return extraCourse;
    }
}
