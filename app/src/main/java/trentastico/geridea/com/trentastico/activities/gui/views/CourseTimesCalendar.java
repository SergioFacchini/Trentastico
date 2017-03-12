package trentastico.geridea.com.trentastico.activities.gui.views;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.android.volley.VolleyError;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import trentastico.geridea.com.trentastico.activities.gui.network.LessonsFetchedListener;
import trentastico.geridea.com.trentastico.activities.gui.network.Networker;
import trentastico.geridea.com.trentastico.activities.model.LessonSchedule;
import trentastico.geridea.com.trentastico.activities.model.LessonsSet;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;
import trentastico.geridea.com.trentastico.activities.utils.AppPreferences;
import trentastico.geridea.com.trentastico.activities.utils.CalendarUtils;

public class CourseTimesCalendar extends CustomWeekView implements DateTimeInterpreter, MonthLoader.MonthChangeListener {

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

    /**
     * True if the next call to onMonthChange will have to load the current events in the calendar.
     */
    private boolean reloadNeeded = true;

    private final LessonsSet currentlyShownLessonsSet = new LessonsSet();

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
        setMonthChangeListener(this);
    }

    public void loadNearEvents() {
        Calendar twoWeeksAgo = CalendarUtils.calculateFirstDayOfWeek();
        twoWeeksAgo.add(Calendar.WEEK_OF_YEAR, -2);

        Calendar twoWeeksFromNow = CalendarUtils.calculateFirstDayOfWeek();
        twoWeeksFromNow.add(Calendar.WEEK_OF_YEAR, +2);

        StudyCourse studyCourse = AppPreferences.getStudyCourse();

        onLoadingOperationStarted.dispatch(null);
        Networker.loadLessonsOfCourse(twoWeeksAgo, twoWeeksFromNow, studyCourse, new LessonsFetchedListener() {
            @Override
            public void onLessonsLoaded(LessonsSet lessons, Calendar from, Calendar to) {
                currentlyShownLessonsSet.mergeWith(lessons);
                setLeftBoundDisabledDay(from);
                setRightBoundDisabledDay(to);

                notifyDatasetChanged();
                onLoadingOperationFinished.dispatch();
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

    @Override
    public void notifyDatasetChanged() {
        reloadNeeded = true;
        super.notifyDatasetChanged();
    }

    @Override
    public String interpretDate(Calendar date) {
        return DATE_FORMAT.format(date.getTime());
    }

    @Override
    public String interpretTime(int hour) {
        return hour+":00";
    }

    @Override
    public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        if (reloadNeeded) {
            reloadNeeded = false;
            return makeEventsFromLessonsSet();
        } else {
            return new ArrayList<WeekViewEvent>();
        }
    }

    private List<? extends WeekViewEvent> makeEventsFromLessonsSet() {
        ArrayList<LessonToEventAdapter> events = new ArrayList<>();
        for (LessonSchedule lessonSchedule : currentlyShownLessonsSet.getScheduledLessons()) {
            events.add(new LessonToEventAdapter(lessonSchedule));
        }

        return events;
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
