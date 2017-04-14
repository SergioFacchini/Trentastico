package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.LessonsSetAvailableListener;
import com.geridea.trentastico.gui.views.requestloader.LessonsLoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class StudyCourseLessonsRequest extends BasicLessonsRequest {

    protected final WeekInterval interval;
    protected final StudyCourse course;
    protected LessonsLoadingListener listener;

    protected boolean areRetrialsEnabled = true;
    protected boolean isCacheCheckEnabled = true;

    protected boolean isRetrying = false;

    public StudyCourseLessonsRequest(WeekInterval interval, StudyCourse course, LessonsLoadingListener listener) {
        this.interval = interval;
        this.course = course;
        this.listener = listener;
    }

    @Override
    public void notifyFailure(final Exception exception, final RequestSender sender) {
        //We had an error trying to load the lessons from the network. We may still
        //have some old cache to try to reuse. In case we do not have such cache, we
        //dispatch the error and keep retrying loading
        if (isRetrying) {
            listener.onErrorHappened(exception, getOperationId());
            resendRequestIfNeeded(sender);
        } else if(isCacheCheckEnabled) {
            final ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
            Cacher.getLessonsInFreshOrDeadCacheAsync(interval, extraCourses, true, new LessonsSetAvailableListener() {
                @Override
                public void onLessonsSetAvailable(CachedLessonsSet cache) {
                    if (cache.isIntervalPartiallyOrFullyCached(interval)) {
                        if (cache.hasMissingIntervals()) {
                            //We found only some pieces. We still return these. To prevent the
                            //request from fetching same events multiple time or making it merge
                            //with maybe deleted events we will make the networker load only the
                            //missing pieces
                            listener.onPartiallyCachedResultsFetched(cache);

                            for (NotCachedInterval notCachedInterval : cache.getMissingIntervals()) {
                                sender.processRequest(notCachedInterval.generateRequest(listener));
                            }

                            listener.onLoadingDelegated(getOperationId());
                        } else {
                            //We found everything we needed in the old cache
                            listener.onLessonsLoaded(cache, interval, 0);
                        }

                    } else {
                        //Nothing found in cache: we keep retrying loading
                        listener.onErrorHappened(exception, getOperationId());
                        resendRequestIfNeeded(sender);
                    }
                }
            });
        } else {
            //Cache check disabled: we just retry to fetch
            listener.onErrorHappened(exception, getOperationId());
            resendRequestIfNeeded(sender);
        }
    }

    @Override
    public void manageResponse(String response, RequestSender sender) {
        try {
            LessonsSet lessonsSet = parseResponse(response);

            //Technically we should always be fetching the latest lesson types. In some cases, however
            //we can scroll back so much to be able to see the previous semesters' courses. We do not
            //want to cache courses that are not actual.
            lessonsSet.removeLessonTypesNotInCurrentSemester();

            onLessonsSetAvailable(lessonsSet);

            Cacher.cacheLessonsSet(lessonsSet, interval);

            listener.onLessonsLoaded(lessonsSet, interval, getOperationId());
        } catch (Exception e) {
            e.printStackTrace();
            BugLogger.logBug();

            listener.onParsingErrorHappened(e, getOperationId());

            resendRequestIfNeeded(sender);
        }
    }

    protected void onLessonsSetAvailable(LessonsSet lessonsSet) {
        //Hook methods for elaborations
    }

    protected void resendRequestIfNeeded(RequestSender sender) {
        if (areRetrialsEnabled) {
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
        listener.onLoadingAboutToStart(new LessonsLoadingMessage(getOperationId(), interval, isRetrying));
    }

    public void setCacheCheckEnabled(boolean isCacheCheckEnabled) {
        this.isCacheCheckEnabled = isCacheCheckEnabled;
    }

    public void setRetrialsEnabled(boolean areRetrialsEnabled) {
        this.areRetrialsEnabled = areRetrialsEnabled;
    }

    @Override
    protected CalendarInterval getCalendarIntervalToLoad() {
        return interval.toCalendarInterval();
    }

    @Override
    protected long getCourseId() {
        return course.getCourseId();
    }

    @Override
    protected int getYear() {
        return course.getYear();
    }
}
