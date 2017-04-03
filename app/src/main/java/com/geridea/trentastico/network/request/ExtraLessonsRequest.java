package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.gui.views.requestloader.ExtraCoursesLoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
import com.geridea.trentastico.utils.time.WeekInterval;

public class ExtraLessonsRequest extends BasicLessonsRequest {

    private final WeekInterval interval;
    private final ExtraCourse extraCourse;

    protected LessonsLoadingListener listener;

    private boolean isRetrying;
    private boolean cacheCheckEnabled;
    private boolean retrialsEnabled;

    public ExtraLessonsRequest(WeekInterval interval, ExtraCourse extraCourse, LessonsLoadingListener listener) {
        this.interval = interval;
        this.listener = listener;
        this.extraCourse = extraCourse;
        this.isRetrying = false;
    }

    @Override
    public void notifyFailure(Exception e, RequestSender sender) {
        listener.onErrorHappened(e, getOperationId());

        //Remember to manage cacheCheckEnabled here when retrieving data from dead cache
        resendRequestIfNeeded(sender);
    }

    @Override
    public void manageResponse(String response, RequestSender sender) {
        try {
            LessonsSet lessonsSet = parseResponse(response);
            lessonsSet.prepareForExtraCourse(extraCourse);

            onLessonsSetAvailable(lessonsSet);

            Cacher.cacheExtraLessonsSet(lessonsSet, getIntervalToLoad(), extraCourse);

            listener.onLessonsLoaded(lessonsSet, getIntervalToLoad(), getOperationId());
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e, getOperationId());

            resendRequestIfNeeded(sender);
        }
    }

    protected void onLessonsSetAvailable(LessonsSet lessonsSet) {
        //hook method for further computations
    }

    private void resendRequestIfNeeded(RequestSender sender) {
        if (retrialsEnabled) {
            isRetrying = true;
            sender.processRequestAfterTimeout(this);
        } else {
            listener.onLoadingAborted(getOperationId());
        }
    }

    @Override
    public void notifyResponseUnsuccessful(int code, RequestSender sender) {
        listener.onErrorHappened(new ResponseUnsuccessfulException(code), getOperationId());

        //In case of error, we resend the request after the timeout
        resendRequestIfNeeded(sender);
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

    public void setCacheCheckEnabled(boolean cacheCheckEnabled) {
        this.cacheCheckEnabled = cacheCheckEnabled;
    }

    public void setRetrialsEnabled(boolean retrialsEnabled) {
        this.retrialsEnabled = retrialsEnabled;
    }
}
