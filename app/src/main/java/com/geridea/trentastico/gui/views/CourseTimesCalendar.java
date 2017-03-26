package com.geridea.trentastico.gui.views;


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekViewEvent;
import com.android.volley.VolleyError;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.LessonsLoader;
import com.geridea.trentastico.network.operations.ILoadingOperation;
import com.geridea.trentastico.network.operations.ParsingErrorOperation;
import com.geridea.trentastico.network.operations.ReadingErrorOperation;
import com.geridea.trentastico.utils.time.CalendarUtils;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Listener1;
import com.threerings.signals.Listener2;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.geridea.trentastico.Config.DEBUG_MODE;

public class CourseTimesCalendar extends CustomWeekView implements CustomWeekView.ScrollListener {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN);
    private final static SimpleDateFormat DATE_FORMAT_DEBUG = new SimpleDateFormat("(w) EEEE d MMMM", Locale.ITALIAN);

    private final static SimpleDateFormat DATE_FORMAT_MEDIUM = new SimpleDateFormat("EE dd/MM", Locale.ITALIAN);
    private final static SimpleDateFormat DATE_FORMAT_MEDIUM_DEBUG = new SimpleDateFormat("(w)EE dd/MM", Locale.ITALIAN);

    private final static SimpleDateFormat DATE_FORMAT_SHORT  = new SimpleDateFormat("dd/MM", Locale.ITALIAN);
    private final static SimpleDateFormat DATE_FORMAT_SHORT_DEBUG  = new SimpleDateFormat("(w)dd/MM", Locale.ITALIAN);

    private final static SimpleDateFormat DATE_FORMAT_SHORT_ONLY_DAY = new SimpleDateFormat("EE", Locale.ITALIAN);

    private final static SimpleDateFormat FORMAT_ONLY_DAY = new SimpleDateFormat("EEEE", Locale.ITALIAN);

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
        setDateTimeInterpreter(getAppropriateDateTimeInterpreter());
        setScrollListener(this);
    }

    private DateTimeInterpreter getAppropriateDateTimeInterpreter() {
        return getDateInterpreterForNumberOfDays(getNumberOfVisibleDays());
    }

    private DateTimeInterpreter getDateInterpreterForNumberOfDays(int numberOfVisibleDays) {
        if(numberOfVisibleDays <= 2){
            return new DateTimeInterpreter() {
                @Override
                public String interpretDate(Calendar date) {
                    Calendar today = Calendar.getInstance();
                    if (isSameDay(today, date)) {
                        return "Oggi (" + FORMAT_ONLY_DAY.format(date.getTime()) + ")";
                    }

                    today.add(Calendar.DAY_OF_MONTH, +1);
                    if (isSameDay(today, date)) {
                        return "Domani (" + FORMAT_ONLY_DAY.format(date.getTime()) + ")";
                    }

                    today.add(Calendar.DAY_OF_MONTH, +1);
                    if (isSameDay(today, date)) {
                        return "Dopodomani (" + FORMAT_ONLY_DAY.format(date.getTime()) + ")";
                    }

                    today.add(Calendar.DAY_OF_MONTH, -3);
                    if (isSameDay(today, date)) {
                        return "Ieri (" + FORMAT_ONLY_DAY.format(date.getTime()) + ")";
                    }

                    return (DEBUG_MODE ? DATE_FORMAT_DEBUG : DATE_FORMAT).format(date.getTime());
                }

                @Override
                public String interpretTime(int hour) {
                    return interpretHours(hour);
                }
            };
        } else if(numberOfVisibleDays <= 5) {
            return new DateTimeInterpreter() {
                @Override
                public String interpretDate(Calendar date) {
                    Calendar today = Calendar.getInstance();
                    if (isSameDay(today, date)) {
                        return "Oggi";
                    }

                    today.add(Calendar.DAY_OF_MONTH, +1);
                    if (isSameDay(today, date)) {
                        return "Domani";
                    }

                    today.add(Calendar.DAY_OF_MONTH, +1);
                    if (isSameDay(today, date)) {
                        return "Dopodomani";
                    }

                    today.add(Calendar.DAY_OF_MONTH, -3);
                    if (isSameDay(today, date)) {
                        return "Ieri";
                    }

                    return (DEBUG_MODE ? DATE_FORMAT_MEDIUM_DEBUG : DATE_FORMAT_MEDIUM)
                                .format(date.getTime());
                }

                @Override
                public String interpretTime(int hour) {
                    return interpretHours(hour);
                }
            };
        } else {// > 6
            return new DateTimeInterpreter() {
                @Override
                public String interpretDate(Calendar date) {
                    Calendar firstDay = CalendarUtils.calculateFirstDayOfWeek();
                    Calendar lastDay  = (Calendar) firstDay.clone();
                    lastDay.add(Calendar.WEEK_OF_MONTH, +1);

                    if (date.equals(firstDay) || (date.after(firstDay) && date.before(lastDay))) {
                        return (DEBUG_MODE ? DATE_FORMAT_SHORT_DEBUG : DATE_FORMAT_SHORT_ONLY_DAY)
                                .format(date.getTime());
                    } else {
                        return (DEBUG_MODE ? DATE_FORMAT_SHORT_DEBUG : DATE_FORMAT_SHORT)
                                    .format(date.getTime());
                    }
                }

                @Override
                public String interpretTime(int hour) {
                    return interpretHours(hour);
                }
            };
        }

    }

    @NonNull
    private String interpretHours(int hour) {
        return hour + ":00";
    }

    private void prepareLoader() {
        loader = new LessonsLoader();
        loader.onLoadingOperationStarted.connect(new Listener1<ILoadingOperation>() {
            @Override
            public void apply(ILoadingOperation operation) {
                onLoadingOperationResult.dispatch(operation);
            }
        });
        loader.onLoadingOperationSuccessful.connect(new Listener2<LessonsSet, WeekInterval>() {
            @Override
            public void apply(LessonsSet lessons, WeekInterval interval) {
                currentlyShownLessonsSet.mergeWith(lessons);

                addEnabledInterval(interval);

                addEventsFromLessonsSet(lessons);

                onLoadingOperationFinished.dispatch();
            }
        });

        loader.onPartiallyCachedResultsFetched.connect(new Listener1<CachedLessonsSet>() {
            @Override
            public void apply(CachedLessonsSet lessonsSet) {
                currentlyShownLessonsSet.mergeWith(lessonsSet);

                ArrayList<WeekInterval> cachedIntervals = lessonsSet.getCachedIntervals();
                addEnabledIntervals(cachedIntervals);

                addEventsFromLessonsSet(lessonsSet);
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

    private void addEnabledIntervals(ArrayList<WeekInterval> intervals) {
        for (WeekInterval interval : intervals) {
            addEnabledInterval(interval);
        }
    }

    public void loadNearEvents() {
        loader.loadEventsNearDay(getFirstVisibleDay());
    }

    private void addEventsFromLessonsSet(LessonsSet lessons) {
        addEvents(makeEventsFromLessonsSet(lessons));
    }

    private boolean isSameDay(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR)         == date2.get(Calendar.YEAR) &&
               date1.get(Calendar.MONTH)        == date2.get(Calendar.MONTH) &&
               date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH);
    }

    private List<? extends WeekViewEvent> makeEventsFromLessonsSet(LessonsSet lessonsSet) {
        ArrayList<LessonToEventAdapter> events = new ArrayList<>();
        for (LessonSchedule lessonSchedule : lessonsSet.getScheduledLessons()) {
            events.add(new LessonToEventAdapter(lessonSchedule));
        }

        return events;
    }

    @Override
    public void onFirstVisibleDayChanged(Calendar newFirst, Calendar oldFirst) {
        if (isInEditMode()) {
            return;
        }

        ScrollDirection scrollDirection = newFirst.after(oldFirst) ? ScrollDirection.RIGHT : ScrollDirection.LEFT;

        CustomWeekView.DisabledDayVisibleResult disabledDays = getDisabledDaysVisibleFromDay(newFirst);
        if(disabledDays.isFirstDayDisabled()) {
            loader.loadDaysIfNeeded(disabledDays.getFirstVisibleWeek(), scrollDirection);
        }

        if(disabledDays.isLastDayDisabled()) {
            loader.loadDaysIfNeeded(disabledDays.getLastVisibleWeek(), scrollDirection);
        }
    }

    public Collection<LessonType> getCurrentLessonTypes() {
        return currentlyShownLessonsSet.getLessonTypes();
    }

    public void notifyLessonTypeVisibilityChanged() {
        clear();

        loader.loadEventsNearDay(getFirstVisibleDay());
    }

    public int rotateNumOfDaysShown() {
        int numOfDaysToShow = getNextNumberOfDaysToShow(getNumberOfVisibleDays());
        prepareForNumberOfVisibleDays(numOfDaysToShow);

        return numOfDaysToShow;
    }

    public void prepareForNumberOfVisibleDays(int numOfDaysToShow) {
        setNumberOfVisibleDays(numOfDaysToShow);
        setDateTimeInterpreter(getAppropriateDateTimeInterpreter());
        invalidate();
    }

    /**
     * @deprecated do not call this method directly, but use
     * {@link CourseTimesCalendar#prepareForNumberOfVisibleDays(int)}
     */
    @Override
    @Deprecated
    public void setNumberOfVisibleDays(int numberOfVisibleDays) {
        super.setNumberOfVisibleDays(numberOfVisibleDays);
    }

    private int getNextNumberOfDaysToShow(int numberOfVisibleDays) {
        switch(numberOfVisibleDays){
            case 1: return 2;
            default:
            case 2: return 3;
            case 3: return 7;
            case 7: return 1;
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
