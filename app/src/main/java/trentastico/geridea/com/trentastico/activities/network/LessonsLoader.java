package trentastico.geridea.com.trentastico.activities.network;

import com.android.volley.VolleyError;
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal3;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import trentastico.geridea.com.trentastico.activities.gui.views.CourseTimesCalendar;
import trentastico.geridea.com.trentastico.activities.model.LessonsSet;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;
import trentastico.geridea.com.trentastico.activities.utils.AppPreferences;
import trentastico.geridea.com.trentastico.activities.utils.CalendarInterval;
import trentastico.geridea.com.trentastico.activities.utils.CalendarUtils;

public class LessonsLoader {

    /**
     * Dispatched when the loading of events has been completed and the calendar can be made
     * visible.
     */
    public final Signal3<LessonsSet, Calendar, Calendar> onLoadingOperationFinished = new Signal3<>();

    /**
     * Dispatched when the calendar starts loading something from internet. The argument is that
     * "something".
     */
    public final Signal1<CourseTimesCalendar.CalendarLoadingOperation> onLoadingOperationStarted = new Signal1<>();

    /**
     * Dispatched when an error happened when trying to fetch lessons.
     */
    public final Signal1<VolleyError> onLoadingErrorHappened = new Signal1<>();

    /**
     * Dispatched when the received response could not be parsed correctly.
     */
    public final Signal1<Exception> onParsingErrorHappened = new Signal1<>();


    private List<CalendarInterval> loadingIntervals = Collections.synchronizedList(new ArrayList<CalendarInterval>());

    public void loadAndAddLessons(final Calendar loadFrom, final Calendar loadTo, StudyCourse studyCourse) {
        onLoadingOperationStarted.dispatch(new CourseTimesCalendar.CalendarLoadingOperation(loadFrom, loadTo));
        addLoadingInterval(loadFrom, loadTo);

        Networker.loadLessonsOfCourse(loadFrom, loadTo, studyCourse, new LessonsFetchedListener() {
            @Override
            public void onLessonsLoaded(LessonsSet lessons, Calendar from, Calendar to) {
                removeLoadingInterval(loadFrom, loadTo);

                onLoadingOperationFinished.dispatch(lessons, from, to);
            }

            @Override
            public void onErrorHappened(VolleyError error) {
                onLoadingErrorHappened.dispatch(error);
            }

            @Override
            public void onParsingErrorHappened(Exception e) {
                onParsingErrorHappened.dispatch(e);
            }
        });
    }

    private void addLoadingInterval(Calendar loadFrom, Calendar loadTo) {
        loadingIntervals.add(new CalendarInterval(loadFrom, loadTo));
    }

    private void removeLoadingInterval(Calendar loadFrom, Calendar loadTo) {
        CalendarInterval intervalToDelete = null;
        for (CalendarInterval loadingInterval : loadingIntervals) {
            if(loadingInterval.matches(loadFrom, loadTo)) {
                intervalToDelete = loadingInterval;
                break;
            }
        }

        loadingIntervals.remove(intervalToDelete);
    }

    private boolean isDayAlreadyBeingLoaded(Calendar day) {
        for (CalendarInterval loadingInterval : loadingIntervals) {
            if (loadingInterval.isInInterval(day)) {
                return true;
            }
        }
        return false;
    }

    public void loadDayOnDayChangeIfNeeded(Calendar newFirstVisibleDay, Calendar oldFirstVisibleDay) {
        if (!isDayAlreadyBeingLoaded(newFirstVisibleDay)) {
            Calendar loadFrom, loadTo;
            if(newFirstVisibleDay.before(oldFirstVisibleDay)){
                //We scrolled backwards
                loadTo = CalendarUtils.calculateFirstDayOfWeek(oldFirstVisibleDay);
                loadTo.add(Calendar.DAY_OF_MONTH, -1);

                loadFrom = (Calendar) loadTo.clone();
                loadFrom.add(Calendar.WEEK_OF_YEAR, -2);
            } else {
                //We scrolled forward
                loadFrom = CalendarUtils.calculateFirstDayOfWeek(newFirstVisibleDay);

                loadTo = (Calendar) loadFrom.clone();
                loadTo.add(Calendar.WEEK_OF_YEAR, +2);
            }

            //Solves a bug when displaying the date to which the times are being loaded
            loadTo.add(Calendar.SECOND, -1);

            loadAndAddLessons(loadFrom, loadTo, AppPreferences.getStudyCourse());
        }
    }

    public void loadNearEvents() {
        Calendar twoWeeksAgo = CalendarUtils.calculateFirstDayOfWeek();
        twoWeeksAgo.add(Calendar.WEEK_OF_YEAR, -1);

        Calendar twoWeeksFromNow = CalendarUtils.calculateFirstDayOfWeek();
        twoWeeksFromNow.add(Calendar.WEEK_OF_YEAR, + 2+1);

        //Solves a bug when displaying the date to which the times are being loaded
        twoWeeksFromNow.add(Calendar.SECOND, -1);

        loadAndAddLessons(twoWeeksAgo, twoWeeksFromNow, AppPreferences.getStudyCourse());
    }
}
