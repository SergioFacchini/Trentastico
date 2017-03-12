package trentastico.geridea.com.trentastico.activities.gui.views;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekViewEvent;
import com.android.volley.VolleyError;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import trentastico.geridea.com.trentastico.activities.gui.network.LessonsFetchedListener;
import trentastico.geridea.com.trentastico.activities.gui.network.Networker;
import trentastico.geridea.com.trentastico.activities.model.LessonSchedule;
import trentastico.geridea.com.trentastico.activities.model.LessonsSet;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;
import trentastico.geridea.com.trentastico.activities.utils.AppPreferences;
import trentastico.geridea.com.trentastico.activities.utils.CalendarInterval;
import trentastico.geridea.com.trentastico.activities.utils.CalendarUtils;

public class CourseTimesCalendar extends CustomWeekView implements DateTimeInterpreter, CustomWeekView.ScrollListener {

    /**
     * Dispatched when the loading of events has been completed and the calendar can be made
     * visible.
     */
    public final Signal0 onLoadingOperationFinished = new Signal0();

    /**
     * Dispatched when the calendar starts loading something from internet. The argument is that
     * "something".
     */
    public final Signal1<CalendarLoadingOperation> onLoadingOperationStarted = new Signal1<>();

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN);

    private final LessonsSet currentlyShownLessonsSet = new LessonsSet();

    private List<CalendarInterval> loadingIntervals = Collections.synchronizedList(new ArrayList<CalendarInterval>());

    public CourseTimesCalendar(Context context) {
        super(context);
        initCalendar();
    }

    public CourseTimesCalendar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCalendar();
    }

    public CourseTimesCalendar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCalendar();
    }

    private void initCalendar() {
        setDateTimeInterpreter(this);
        setScrollListener(this);
    }

    public void loadNearEvents() {
        Calendar twoWeeksAgo = CalendarUtils.calculateFirstDayOfWeek();
        twoWeeksAgo.add(Calendar.WEEK_OF_YEAR, -1);

        Calendar twoWeeksFromNow = CalendarUtils.calculateFirstDayOfWeek();
        twoWeeksFromNow.add(Calendar.WEEK_OF_YEAR, + 2+1);

        loadAndAddLessons(twoWeeksAgo, twoWeeksFromNow, AppPreferences.getStudyCourse());
    }

    private void loadAndAddLessons(final Calendar loadFrom, final Calendar loadTo, StudyCourse studyCourse) {
        onLoadingOperationStarted.dispatch(null);
        addLoadingInterval(loadFrom, loadTo);

        Networker.loadLessonsOfCourse(loadFrom, loadTo, studyCourse, new LessonsFetchedListener() {
            @Override
            public void onLessonsLoaded(LessonsSet lessons, Calendar from, Calendar to) {
                currentlyShownLessonsSet.mergeWith(lessons);
                extendLeftBoundDisabledDay(from);
                extendRightBoundDisabledDay(to);

                removeLoadingInterval(loadFrom, loadTo);

                onLoadingOperationFinished.dispatch();

                addEventsFromLessonsSet(lessons);
            }

            @Override
            public void onErrorHappened(VolleyError error) {
                Toast.makeText(getContext(), "Error happened",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onParsingErrorHappened(Exception e) {
                Toast.makeText(getContext(), "Parsing error happened",Toast.LENGTH_SHORT).show();
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

    private void addEventsFromLessonsSet(LessonsSet lessons) {
        addEvents(makeEventsFromLessonsSet(lessons));
    }

    @Override
    public String interpretDate(Calendar date) {
        return DATE_FORMAT.format(date.getTime());
    }

    @Override
    public String interpretTime(int hour) {
        return hour+":00";
    }

    private List<? extends WeekViewEvent> makeEventsFromLessonsSet(LessonsSet lessonsSet) {
        ArrayList<LessonToEventAdapter> events = new ArrayList<>();
        for (LessonSchedule lessonSchedule : lessonsSet.getScheduledLessons()) {
            events.add(new LessonToEventAdapter(lessonSchedule));
        }

        return events;
    }

    @Override
    public void onFirstVisibleDayChanged(Calendar newFirstVisibleDay, Calendar oldFirstVisibleDay) {
        if(isADisabledDay(newFirstVisibleDay) && !isDayAlreadyBeingLoaded(newFirstVisibleDay)){
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

            loadAndAddLessons(loadFrom, loadTo, AppPreferences.getStudyCourse());
        }

    }

    private boolean isDayAlreadyBeingLoaded(Calendar day) {
        for (CalendarInterval loadingInterval : loadingIntervals) {
            if (loadingInterval.isInInterval(day)) {
                return true;
            }
        }
        return false;
    }

    public static class LessonToEventAdapter extends WeekViewEvent {

        private static int PROGRESSIVE_ID = 1;

        private LessonSchedule lesson;

        public LessonToEventAdapter(LessonSchedule lesson) {
            super(PROGRESSIVE_ID++, lesson.getFullDescription(), lesson.getStartCal(), lesson.getEndCal());
            setColor(lesson.getColor());

            this.lesson = lesson;
        }

        public LessonSchedule getLesson() {
            return lesson;
        }
    }

    public class CalendarLoadingOperation {

    }
}
