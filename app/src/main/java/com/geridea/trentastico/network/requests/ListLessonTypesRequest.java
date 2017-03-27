package com.geridea.trentastico.network.requests;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.android.volley.VolleyError;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

public class ListLessonTypesRequest extends AbstractServerRequest {

    /**
     * Dispatched when the request has been successfully fulfilled.
     */
    public final Signal0 onRequestSuccessful = new Signal0();

    /**
     * Dispatched when the request has encountered an error while trying to parse the response.
     */
    public final Signal1<Exception> onParsingErrorHappened = new Signal1<>();
    /**
     * Dispatched when there is an error while trying to get lessons from internet.
     */
    public final Signal1<VolleyError> onNetworkErrorHappened = new Signal1<>();


    private final StudyCourse studyCourse;
    private final Networker.CoursesOfStudyCourseListener listener;
    private final WeekInterval weekInterval;

    public ListLessonTypesRequest(StudyCourse studyCourse, Networker.CoursesOfStudyCourseListener listener) {
        this.studyCourse = studyCourse;
        this.listener = listener;
        this.weekInterval = new WeekInterval(-2, +2);
    }

    @Override
    public void deliverError(VolleyError error) {
        listener.onErrorHappened(error);
        onNetworkErrorHappened.dispatch(error);
    }

    @Override
    public StudyCourse getStudyCourse() {
        return studyCourse;
    }

    @Override
    public WeekInterval getIntervalToLoad() {
        return weekInterval;
    }

    @Override
    public void onResponse(String response) {
        try {
            LessonsSet lessonsSet = parseResponse(response);
            listener.onLessonTypesRetrieved(lessonsSet.getLessonTypes());
            onRequestSuccessful.dispatch();
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e);
            onParsingErrorHappened.dispatch(e);
        }

    }
}
