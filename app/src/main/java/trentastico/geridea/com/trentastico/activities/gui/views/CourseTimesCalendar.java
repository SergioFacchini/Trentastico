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
import com.threerings.signals.Listener1;
import com.threerings.signals.Listener3;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import trentastico.geridea.com.trentastico.activities.model.LessonSchedule;
import trentastico.geridea.com.trentastico.activities.model.LessonsSet;
import trentastico.geridea.com.trentastico.activities.network.LessonsLoader;

public class CourseTimesCalendar extends CustomWeekView implements DateTimeInterpreter, CustomWeekView.ScrollListener {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN);

    //Signals
    public final Signal1<CalendarLoadingOperation> onLoadingOperationStarted = new Signal1<>();
    public final Signal0 onLoadingOperationFinished = new Signal0();

    //Data
    private final LessonsSet currentlyShownLessonsSet = new LessonsSet();
    private LessonsLoader loader;

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
        prepareLoader();

        setDateTimeInterpreter(this);
        setScrollListener(this);
    }

    private void prepareLoader() {
        loader = new LessonsLoader();
        loader.onLoadingOperationStarted.connect(new Listener1<CalendarLoadingOperation>() {
            @Override
            public void apply(CalendarLoadingOperation operation) {
                onLoadingOperationStarted.dispatch(operation);
            }
        });
        loader.onLoadingOperationFinished.connect(new Listener3<LessonsSet, Calendar, Calendar>() {
            @Override
            public void apply(LessonsSet lessons, Calendar from, Calendar to) {
                currentlyShownLessonsSet.mergeWith(lessons);
                extendLeftBoundDisabledDay(from);
                extendRightBoundDisabledDay(to);

                addEventsFromLessonsSet(lessons);

                onLoadingOperationFinished.dispatch();
            }
        });
        loader.onLoadingErrorHappened.connect(new Listener1<VolleyError>() {
            @Override
            public void apply(VolleyError error) {
                Toast.makeText(getContext(), "Error happened",Toast.LENGTH_SHORT).show();
            }
        });
        loader.onParsingErrorHappened.connect(new Listener1<Exception>() {
            @Override
            public void apply(Exception e) {
                Toast.makeText(getContext(), "Parsing error happened",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadNearEvents() {
        loader.loadNearEvents();
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
        if(isADisabledDay(newFirstVisibleDay)){
            loader.loadDayOnDayChangeIfNeeded(newFirstVisibleDay, oldFirstVisibleDay);
        }
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

    public static class CalendarLoadingOperation {
        private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.ITALIAN);

        private Calendar from, to;

        public CalendarLoadingOperation(Calendar from, Calendar to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return String.format(
                    "Sto caricando gli orari dal %s al %s...",
                    DATE_FORMAT.format(from.getTime()),
                    DATE_FORMAT.format(to.getTime())
            );
        }
    }
}
