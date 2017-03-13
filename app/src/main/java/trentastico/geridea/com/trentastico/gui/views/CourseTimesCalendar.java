package trentastico.geridea.com.trentastico.gui.views;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.util.AttributeSet;

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

import trentastico.geridea.com.trentastico.model.LessonSchedule;
import trentastico.geridea.com.trentastico.model.LessonsSet;
import trentastico.geridea.com.trentastico.network.operations.ILoadingOperation;
import trentastico.geridea.com.trentastico.network.LessonsLoader;
import trentastico.geridea.com.trentastico.network.operations.ParsingErrorOperation;
import trentastico.geridea.com.trentastico.network.operations.ReadingErrorOperation;

public class CourseTimesCalendar extends CustomWeekView implements DateTimeInterpreter, CustomWeekView.ScrollListener {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN);

    //Signals
    /**
     * Dispatched when the calendar is about to perform a loading from the network.<br>
     * WARNING: may be called on a not-UI thread!
     */
    public final Signal1<ILoadingOperation> onLoadingOperationResult = new Signal1<>();

    /**
     * Dispatched when the calendar has finished loading something from the network.<br>
     * WARNING: may be called on a not-UI thread!
     */
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
        loader.onLoadingOperationStarted.connect(new Listener1<ILoadingOperation>() {
            @Override
            public void apply(ILoadingOperation operation) {
                onLoadingOperationResult.dispatch(operation);
            }
        });
        loader.onLoadingOperationSuccessful.connect(new Listener3<LessonsSet, Calendar, Calendar>() {
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
                onLoadingOperationResult.dispatch(new ReadingErrorOperation());
            }
        });
        loader.onParsingErrorHappened.connect(new Listener1<Exception>() {
            @Override
            public void apply(Exception e) {
                e.printStackTrace();
                onLoadingOperationResult.dispatch(new ParsingErrorOperation());
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
        private LessonSchedule lesson;

        public LessonToEventAdapter(LessonSchedule lesson) {
            super(lesson.getId(), lesson.getFullDescription(), lesson.getStartCal(), lesson.getEndCal());
            setColor(lesson.getColor());

            this.lesson = lesson;
        }

        public LessonSchedule getLesson() {
            return lesson;
        }
    }

}
