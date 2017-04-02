package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.request.listener.ListLessonsListener;
import com.geridea.trentastico.utils.time.WeekInterval;


public class ListLessonsRequest extends BasicLessonsRequest {

    private final StudyCourse studyCourse;
    private final ListLessonsListener listener;
    private final WeekInterval weekInterval;

    public ListLessonsRequest(StudyCourse studyCourse, ListLessonsListener listener) {
        this.studyCourse = studyCourse;
        this.listener = listener;
        this.weekInterval = new WeekInterval(-2, +2);
    }

    @Override
    public void notifyFailure(Exception e, RequestSender sender) {
        listener.onErrorHappened(e);

    }

    @Override
    public void manageResponse(String response, RequestSender sender) {
        try {
            LessonsSet lessonsSet = parseResponse(response);
            listener.onLessonTypesRetrieved(lessonsSet.getLessonTypes());
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e);
        }

    }

    @Override
    public void notifyResponseUnsuccessful(int code, RequestSender sender) {
        listener.onErrorHappened(new ResponseUnsuccessfulException(code));

    }

    @Override
    public void notifyOnBeforeSend() {
        //Nothing to do
    }

    @Override
    protected WeekInterval getIntervalToLoad() {
        return weekInterval;
    }

    @Override
    protected StudyCourse getStudyCourse() {
        return studyCourse;
    }
}
