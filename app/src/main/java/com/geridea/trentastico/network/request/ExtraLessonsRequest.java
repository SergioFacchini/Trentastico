package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.network.operations.ExtraCoursesLoadingMessage;
import com.geridea.trentastico.utils.time.WeekInterval;

public class ExtraLessonsRequest extends BasicLessonsRequest {

    private final WeekInterval interval;
    private final LessonsLoadingListener listener;
    private final ExtraCourse extraCourse;
    private boolean isRetrying;

    public ExtraLessonsRequest(WeekInterval interval, LessonsLoadingListener listener, ExtraCourse extraCourse) {
        this.interval = interval;
        this.listener = listener;
        this.extraCourse = extraCourse;
        this.isRetrying = false;
    }

    @Override
    public void notifyFailure(Exception e, RequestSender sender) {
        listener.onErrorHappened(e, getOperationId());

        retrySendingRequest(sender);
    }

    @Override
    public void manageResponse(String response, RequestSender sender) {
        try {
            LessonsSet lessonsSet = parseResponse(response);
            lessonsSet.prepareForExtraCourse(extraCourse);

            Cacher.cacheExtraLessonsSet(lessonsSet, getIntervalToLoad(), extraCourse);

            listener.onLessonsLoaded(lessonsSet, getIntervalToLoad(), getOperationId());
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e, getOperationId());

            retrySendingRequest(sender);
        }
    }

    private void retrySendingRequest(RequestSender sender) {
        isRetrying = true;
        sender.processRequestAfterTimeout(this);
    }

    @Override
    public void notifyResponseUnsuccessful(int code, RequestSender sender) {
        listener.onErrorHappened(new ResponseUnsuccessfulException(code), getOperationId());

        //In case of error, we resend the request after the timeout
        retrySendingRequest(sender);
    }

    @Override
    public void notifyOnBeforeSend() {
        listener.onLoadingAboutToStart(new ExtraCoursesLoadingMessage(this));
    }

    @Override
    public WeekInterval getIntervalToLoad() {
        return interval;
    }

    @Override
    public StudyCourse getStudyCourse() {
        return new StudyCourse(-1, extraCourse.getCourseId(), extraCourse.getYear());
    }

    public ExtraCourse getExtraCourse() {
        return extraCourse;
    }

    public boolean isRetrying() {
        return isRetrying;
    }
}
