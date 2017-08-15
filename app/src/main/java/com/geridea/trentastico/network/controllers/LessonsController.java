package com.geridea.trentastico.network.controllers;


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.LessonsSetAvailableListener;
import com.geridea.trentastico.database.NotCachedIntervalsListener;
import com.geridea.trentastico.database.TodaysLessonsListener;
import com.geridea.trentastico.gui.views.requestloader.ExtraCoursesLoadingMessage;
import com.geridea.trentastico.gui.views.requestloader.LessonsLoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.CourseAndYear;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.CachedInterval;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.LoadingIntervalKnownListener;
import com.geridea.trentastico.network.controllers.listener.DiffCompletedListener;
import com.geridea.trentastico.network.controllers.listener.LessonWithRoomFetchedListener;
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener;
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.controllers.listener.LessonsWithRoomListener;
import com.geridea.trentastico.network.controllers.listener.LessonsWithRoomMultipleListener;
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener;
import com.geridea.trentastico.network.controllers.listener.WaitForDownloadLessonListener;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.network.request.ResponseUnsuccessfulException;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekDayTime;
import com.geridea.trentastico.utils.time.WeekInterval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.FormBody;

public class LessonsController extends BasicController {

    private static int PROGRESSIVE_OPERATION_ID_COUNTER = 1;

    public LessonsController(RequestSender sender, Cacher cacher) {
        super(sender, cacher);
    }

    public void loadRoomsForLessonsIfMissing(ArrayList<LessonSchedule> lessonsToFilter, LessonsWithRoomListener listener) {
        //Finding lessons with and without rooms
        ArrayList<LessonSchedule> lessonsToLoad   = new ArrayList<>();
        ArrayList<LessonSchedule> lessonsWithRoom = new ArrayList<>();
        for (LessonSchedule lesson : lessonsToFilter) {
            if (lesson.hasRoomSpecified()) {
                lessonsWithRoom.add(lesson);
            } else {
                lessonsToLoad.add(lesson);
            }
        }

        //If there are no lessons to load, we just return them
        if (lessonsToLoad.isEmpty()) {
            listener.onLoadingCompleted(lessonsToFilter, new ArrayList<LessonSchedule>());
        } else {
            LessonsWithRoomMultipleListener fetchRoomListener
                    = new LessonsWithRoomMultipleListener(lessonsToLoad, lessonsWithRoom,listener);

            for (LessonSchedule lesson: lessonsToLoad) {
                CourseAndYear cay = LessonSchedule.findCourseAndYearForLesson(lesson);
                sender.processRequest(new FetchRoomForLessonRequest(lesson, cay, fetchRoomListener));
            }

        }
    }

    public void getTodaysCachedLessons(TodaysLessonsListener todaysLessonsListener) {
        cacher.getTodaysCachedLessons(todaysLessonsListener);
    }

    private static String buildRequestURL(long courseId, int year, CalendarInterval intervalToLoad) {
        if(Config.INSTANCE.getDEBUG_MODE() && Config.INSTANCE.getLAUNCH_REQUESTS_TO_DEBUG_SERVER()){
            return Config.INSTANCE.getDEBUG_SERVER_URL();
        }

        return String.format(
                Locale.CANADA,
                "http://webapps.unitn.it/Orari/it/Web/AjaxEventi/c/%d-%d/agendaWeek?_=%d&start=%d&end=%d",
                courseId,
                year,
                System.currentTimeMillis(),
                intervalToLoad.getFrom().getTimeInMillis() / 1000,
                intervalToLoad.getTo()  .getTimeInMillis() / 1000
        );
    }

    public void loadLessons(final WeekInterval intervalToLoad, final LessonsLoadingListener listener,
                            final LoadingIntervalKnownListener intervalListener) {

        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        cacher.getLessonsInFreshOrDeadCacheAsync(intervalToLoad, extraCourses, false, new LessonsSetAvailableListener() {
            @Override
            public void onLessonsSetAvailable(CachedLessonsSet cacheSet) {
                if (cacheSet.hasMissingIntervals()) {
                    if(cacheSet.wereSomeLessonsFoundInCache()){
                        listener.onPartiallyCachedResultsFetched(cacheSet);
                    } else {
                        listener.onNothingFoundInCache();
                    }

                    for (NotCachedInterval interval: cacheSet.getMissingIntervals()) {
                        interval.launchLoading(LessonsController.this, listener);
                    }
                } else {
                    //We found everything we needed in cache
                    listener.onLessonsLoaded(cacheSet, intervalToLoad, 0);
                }

                intervalListener.onIntervalsToLoadKnown(intervalToLoad, cacheSet.getMissingIntervals());
            }
        });
    }

    public void diffLessonsInCache(final WeekInterval intervalToCheck, final LessonsDifferenceListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        cacher.getLessonsInFreshOrDeadCacheAsync(intervalToCheck, extraCourses, false, new LessonsSetAvailableListener() {
            @Override
            public void onLessonsSetAvailable(CachedLessonsSet cacheSet) {
                if(cacheSet.isIntervalPartiallyOrFullyCached(intervalToCheck)){
                    ArrayList<CachedInterval> cachedIntervals = cacheSet.getCachedIntervals();

                    listener.onNumberOfRequestToSendKnown(cachedIntervals.size());

                    for (CachedInterval cachedInterval: cachedIntervals) {
                        cachedInterval.launchDiffRequest(LessonsController.this, listener);
                    }
                } else {
                    listener.onNoLessonsInCache();
                }
            }
        });
    }

    public void loadAndCacheNotCachedLessons(WeekInterval interval, final WaitForDownloadLessonListener listener) {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        cacher.getNotCachedSubintervals(interval, extraCourses, new NotCachedIntervalsListener(){

            public void onIntervalsKnown(ArrayList<NotCachedInterval> notCachedIntervals ){
                if (notCachedIntervals.isEmpty()) {
                    listener.onNothingToLoad();
                } else {
                    for (NotCachedInterval notCachedInterval: notCachedIntervals) {
                        notCachedInterval.launchLoadingOneTime(LessonsController.this, listener);
                    }
                }
            }
        });
    }

    public void loadCoursesOfStudyCourse(StudyCourse studyCourse, ListLessonsListener listener) {
        sender.processRequest(new ListLessonsRequest(studyCourse, listener));
    }

    public void loadTodaysLessons(final TodaysLessonsListener todaysLessonsListener) {
        final WeekDayTime today = new WeekDayTime();

        //Here we have to load all the lesson scheduled for today.
        WeekInterval dayInterval = today.getContainingInterval();
        loadAndCacheNotCachedLessons(dayInterval, new WaitForDownloadLessonListener() {
            @Override
            public void onFinish(boolean loadingSuccessful) {
                if (loadingSuccessful) {
                    cacher.getTodaysCachedLessons(todaysLessonsListener);
                } else {
                    todaysLessonsListener.onLessonsCouldNotBeLoaded();
                }
            }
        });
    }

    public void removeExtraCoursesWithLessonType(int lessonTypeId) {
        cacher.removeExtraCoursesWithLessonType(lessonTypeId);
    }

    public void sendExtraCourseLoadingRequest(WeekInterval interval, ExtraCourse course, LessonsLoadingListener listener) {
        sender.processRequest(generateExtraLessonRequest(interval, course, listener));
    }

    public void sendExtraCourseLoadingRequestOneTime(WeekInterval interval, ExtraCourse extraCourse, LessonsLoadingListener listener) {
        ExtraLessonsRequest request = generateExtraLessonRequest(interval, extraCourse, listener);
        request.setCacheCheckEnabled(false);
        request.setRetrialsEnabled(false);

        sender.processRequest(request);
    }

    @NonNull
    private ExtraLessonsRequest generateExtraLessonRequest(WeekInterval interval, ExtraCourse extraCourse, LessonsLoadingListener listener) {
        return new ExtraLessonsRequest(interval, extraCourse, listener);
    }

    public void sendStudyCourseLoadingRequest(WeekInterval interval, LessonsLoadingListener listener) {
        sender.processRequest(generateStudyCourseLessonRequest(interval, listener));
    }

    public void sendStudyCourseLoadingRequestOneTime(WeekInterval interval, LessonsLoadingListener listener) {
        StudyCourseLessonsRequest request = generateStudyCourseLessonRequest(interval, listener);
        request.setRetrialsEnabled(false);
        request.setCacheCheckEnabled(false);

        sender.processRequest(request);
    }

    private StudyCourseLessonsRequest generateStudyCourseLessonRequest(WeekInterval interval, LessonsLoadingListener listener) {
        return new StudyCourseLessonsRequest(interval, AppPreferences.getStudyCourse(), listener);
    }

    public void diffExtraCourseLessons(WeekInterval interval, ExtraCourse course,
                                       ArrayList<LessonSchedule> cachedLessons, LessonsDifferenceListener listener) {

        sender.processRequest(new ExtraCourseLessonsDiffRequest(interval, course, cachedLessons, listener));
    }

    public void obliterateAllLessonsCache() {
        cacher.obliterateAllLessonsCache();
    }

    public void purgeStudyCourseCache() {
        cacher.purgeStudyCourseCache();
    }

    public void diffStudyCourseLessons(WeekInterval interval, StudyCourse course,
                                       ArrayList<LessonSchedule> lessons, LessonsDifferenceListener listener) {
        sender.processRequest(new StudyCourseLessonsDiffRequest(interval, course, lessons, listener));
    }

    //////////////////////////////////////
    // Requests
    /////////////////////////////////////
    abstract class BasicLessonsRequest implements IRequest {

        private final int operationId;

        public BasicLessonsRequest() {
            this.operationId = PROGRESSIVE_OPERATION_ID_COUNTER++;
        }

        @Override
        public String getURL() {
            return buildRequestURL(getCourseId(), getYear(), getCalendarIntervalToLoad());
        }

        protected abstract CalendarInterval getCalendarIntervalToLoad();
        protected abstract long getCourseId();
        protected abstract int getYear();

        @NonNull
        protected LessonsSet parseResponse(String response) throws JSONException {
            JSONObject jsonResponse = new JSONObject(response);

            LessonsSet lessonsSet = new LessonsSet();
            JSONArray activitiesJson = jsonResponse.getJSONArray("Attivita");
            parseLessonTypes(lessonsSet, activitiesJson);

            JSONArray lessonsJson = jsonResponse.getJSONArray("Eventi");
            lessonsSet.addLessonSchedules(createLessonSchedulesFromJSON(lessonsJson));
            return lessonsSet;
        }

        private void parseLessonTypes(LessonsSet lessonsSet, JSONArray activitiesJson) throws JSONException {
            for(int i = 0; i<activitiesJson.length(); i++){
                lessonsSet.addLessonType(LessonType.fromJson(activitiesJson.getJSONObject(i)));
            }
        }

        @NonNull
        private ArrayList<LessonSchedule> createLessonSchedulesFromJSON(JSONArray eventsJson) throws JSONException {
            ArrayList<LessonSchedule> schedules = new ArrayList<>();
            for(int i = 0; i<eventsJson.length(); i++){
                schedules.add(LessonSchedule.fromJson(eventsJson.getJSONObject(i)));
            }
            return schedules;
        }

        public int getOperationId() {
            return operationId;
        }

        @Nullable
        @Override
        public FormBody getFormToSend() {
            //We have nothing to send
            return null;
        }
    }

    private class FetchRoomForLessonRequest extends BasicLessonsRequest {

        private final LessonSchedule lesson;
        private final CourseAndYear cay;
        private final LessonWithRoomFetchedListener listener;

        public FetchRoomForLessonRequest(LessonSchedule lesson, CourseAndYear cay, LessonWithRoomFetchedListener listener) {
            this.lesson = lesson;
            this.cay = cay;
            this.listener = listener;
        }

        @Override
        public void notifyFailure(Exception e, RequestSender sender) {
            //In this request we just don't manage errors
            listener.onError(lesson);
        }

        @Override
        public void manageResponse(String response, RequestSender sender) {
            try {
                LessonsSet lessonsSet = parseResponse(response);
                for (LessonSchedule fetchedLesson : lessonsSet.getScheduledLessons()) {
                    if (fetchedLesson.getId() == lesson.getId()) {
                        lesson.setRoom(fetchedLesson.getRoom());
                        break;
                    }
                }

                //Note: here might happen that the lesson we were trying to fetch the room for is not
                //available; actually this happens were rarely, when we try to get the lesson's room
                //but exactly at that time the lessons gets removed. In this case we just keep the
                //lesson as it is, even though the best way to handle this would be to not consider that
                //lesson for further elaborations.
                listener.onUpdateSuccessful(lesson);
            } catch (JSONException e) {
                notifyFailure(e, sender);
            }

        }

        @Override
        public void notifyResponseUnsuccessful(int code, RequestSender sender) {
            listener.onError(lesson);
        }

        @Override
        public void notifyOnBeforeSend() { }

        @Override
        protected CalendarInterval getCalendarIntervalToLoad() {
            //For some reasons, trying to fetch too small intervals does not returns us any result!
            return lesson.toExpandedCalendarInterval(Calendar.HOUR_OF_DAY, 4);
        }

        @Override
        protected long getCourseId() {
            return cay.courseId;
        }

        @Override
        protected int getYear() {
            return cay.year;
        }

    }

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
                cacher.getLessonsInFreshOrDeadCacheAsync(interval, extraCourses, true, new LessonsSetAvailableListener() {
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
                                    notCachedInterval.launchLoading(LessonsController.this, listener);
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

                cacher.cacheLessonsSet(lessonsSet, interval);

                onLessonsSetAvailable(lessonsSet);

                listener.onLessonsLoaded(lessonsSet, interval, getOperationId());
            } catch (Exception e) {
                e.printStackTrace();
                BugLogger.logBug("Parsing study course request", e);

                listener.onParsingErrorHappened(e, getOperationId());

                resendRequestIfNeeded(sender);
            }
        }

        protected void onLessonsSetAvailable(LessonsSet lessonsSet) {
            //Hook methods for elaborations
        }

        void resendRequestIfNeeded(RequestSender sender) {
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

    private class StudyCourseLessonsDiffRequest extends StudyCourseLessonsRequest {

        private final ArrayList<LessonSchedule> cachedLessons;
        private final LessonsDifferenceListener differenceListener;

        public StudyCourseLessonsDiffRequest(
                WeekInterval interval, StudyCourse course, ArrayList<LessonSchedule> cachedLessons,
                LessonsDifferenceListener differenceListener) {

            super(interval, course, new DiffCompletedListener(differenceListener));

            this.cachedLessons = cachedLessons;
            this.differenceListener = differenceListener;

            setRetrialsEnabled(false);
            setCacheCheckEnabled(false);
        }

        @Override
        protected void onLessonsSetAvailable(LessonsSet lessonsSet) {
            ArrayList<LessonSchedule> fetchedLessons = new ArrayList<>(lessonsSet.getScheduledLessons());

            //Do not compare what we filtered
            LessonSchedule.filterLessons(fetchedLessons);
            LessonSchedule.filterLessons(cachedLessons);

            differenceListener.onDiffResult(LessonSchedule.diffLessons(cachedLessons, fetchedLessons));
        }

    }

    private class ExtraCourseLessonsDiffRequest extends ExtraLessonsRequest {

        private final ArrayList<LessonSchedule> cachedLessons;
        private final LessonsDifferenceListener differenceListener;

        public ExtraCourseLessonsDiffRequest(WeekInterval interval, ExtraCourse extraCourse, ArrayList<LessonSchedule> cachedLessons, LessonsDifferenceListener differenceListener) {
            super(interval, extraCourse, new DiffCompletedListener(differenceListener));
            this.cachedLessons = cachedLessons;
            this.differenceListener = differenceListener;

            setCacheCheckEnabled(false);
            setRetrialsEnabled(false);

        }

        @Override
        protected void onLessonsSetAvailable(LessonsSet lessonsSet) {
            ArrayList<LessonSchedule> fetchedLessons = new ArrayList<>(lessonsSet.getScheduledLessons());

            //Do not compare what we filtered
            LessonSchedule.filterLessons(fetchedLessons);
            LessonSchedule.filterLessons(cachedLessons);

            differenceListener.onDiffResult(LessonSchedule.diffLessons(cachedLessons, fetchedLessons));
        }

    }

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

                cacher.cacheExtraLessonsSet(lessonsSet, getIntervalToLoad(), extraCourse);

                listener.onLessonsLoaded(lessonsSet, getIntervalToLoad(), getOperationId());
            } catch (Exception e) {
                e.printStackTrace();
                BugLogger.logBug("Parsing extra lessons request", e);

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

        public WeekInterval getIntervalToLoad() {
            return interval;
        }

        @Override
        protected CalendarInterval getCalendarIntervalToLoad() {
            return interval.toCalendarInterval();
        }

        @Override
        protected long getCourseId() {
            return extraCourse.getCourseId();
        }

        @Override
        protected int getYear() {
            return extraCourse.getYear();
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
                BugLogger.logBug("Parsing lessons list request", e);

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
        protected CalendarInterval getCalendarIntervalToLoad() {
            return weekInterval.toCalendarInterval();
        }

        @Override
        protected long getCourseId() {
            return studyCourse.getCourseId();
        }

        @Override
        protected int getYear() {
            return studyCourse.getYear();
        }

    }
}
