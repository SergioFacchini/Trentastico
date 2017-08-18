package com.geridea.trentastico.gui.views


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context
import android.util.AttributeSet
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekViewEvent
import com.geridea.trentastico.Config
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.LessonsSet
import com.geridea.trentastico.network.LessonsLoader
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import com.geridea.trentastico.utils.time.WeekInterval
import com.threerings.signals.Signal1
import java.text.SimpleDateFormat
import java.util.*

class CourseTimesCalendar : CustomWeekView, CustomWeekView.ScrollListener {

    //Signals
    /**
     * Dispatched when something worth of note is happening in the calendar. For instance starting
     * or finishing fetching lessons from network.<br></br>
     * WARNING: may be called on a not-UI thread!
     */
    val onLoadingOperationNotify = Signal1<ILoadingMessage>()

    //Data
    private val currentlyShownLessonsSet = LessonsSet()
    private var loader: LessonsLoader? = null

    constructor(context: Context) : super(context) {
        initCalendar()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initCalendar()
    }

    constructor(
            context: Context,
            attrs: AttributeSet,
            defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initCalendar()
    }

    private fun initCalendar() {
        prepareLoader()
        dateTimeInterpreter = appropriateDateTimeInterpreter
        scrollListener = this
    }

    private val appropriateDateTimeInterpreter: DateTimeInterpreter
        get() = getDateInterpreterForNumberOfDays(numberOfVisibleDays)

    private fun getDateInterpreterForNumberOfDays(numberOfVisibleDays: Int): DateTimeInterpreter =
            if (numberOfVisibleDays <= 2) {
                object : DateTimeInterpreter {
                    override fun interpretDate(date: Calendar): String {
                        val today = CalendarUtils.debuggableToday
                        if (isSameDay(today, date)) {
                            return "Oggi (" + FORMAT_ONLY_DAY.format(date.time) + ")"
                        }

                        today.add(Calendar.DAY_OF_MONTH, +1)
                        if (isSameDay(today, date)) {
                            return "Domani (" + FORMAT_ONLY_DAY.format(date.time) + ")"
                        }

                        today.add(Calendar.DAY_OF_MONTH, +1)
                        if (isSameDay(today, date)) {
                            return "Dopodomani (" + FORMAT_ONLY_DAY.format(date.time) + ")"
                        }

                        today.add(Calendar.DAY_OF_MONTH, -3)
                        return if (isSameDay(today, date)) {
                            "Ieri (" + FORMAT_ONLY_DAY.format(date.time) + ")"
                        } else (if (Config.DEBUG_MODE) DATE_FORMAT_DEBUG else DATE_FORMAT).format(date.time)

                    }

                    override fun interpretTime(hour: Int): String = interpretHours(hour)
                }
            } else if (numberOfVisibleDays <= 5) {
                object : DateTimeInterpreter {
                    override fun interpretDate(date: Calendar): String {
                        val today = CalendarUtils.debuggableToday
                        if (isSameDay(today, date)) {
                            return "Oggi"
                        }

                        today.add(Calendar.DAY_OF_MONTH, +1)
                        if (isSameDay(today, date)) {
                            return "Domani"
                        }

                        today.add(Calendar.DAY_OF_MONTH, +1)
                        if (isSameDay(today, date)) {
                            return "Dopodomani"
                        }

                        today.add(Calendar.DAY_OF_MONTH, -3)
                        if (isSameDay(today, date)) {
                            return "Ieri"
                        }

                        return if (Config.DEBUG_MODE)
                            DATE_FORMAT_MEDIUM_DEBUG.format(date.time)
                        else
                            DATE_FORMAT_MEDIUM.format(date.time)
                    }

                    override fun interpretTime(hour: Int): String = interpretHours(hour)
                }
            } else {
                // > 6
                object : DateTimeInterpreter {
                    override fun interpretDate(date: Calendar): String {
                        val firstDay = CalendarUtils.calculateFirstDayOfWeek()
                        val lastDay = firstDay.clone() as Calendar
                        lastDay.add(Calendar.WEEK_OF_MONTH, +1)

                        return if (date == firstDay || date.after(firstDay) && date.before(lastDay)) {
                            if (Config.DEBUG_MODE)
                                DATE_FORMAT_SHORT_DEBUG.format(date.time)
                            else
                                DATE_FORMAT_SHORT_ONLY_DAY.format(date.time)
                        } else {
                            if (Config.DEBUG_MODE)
                                DATE_FORMAT_SHORT_DEBUG.format(date.time)
                            else
                                DATE_FORMAT_SHORT.format(date.time)
                        }
                    }

                    override fun interpretTime(hour: Int): String = interpretHours(hour)
                }
            }

    private fun interpretHours(hour: Int): String = hour.toString()

    private fun prepareLoader() {
        loader = LessonsLoader()
        loader!!.onLoadingMessageDispatched.connect { message -> onLoadingOperationNotify.dispatch(message) }
        loader!!.onLoadingOperationSuccessful.connect { lessons, interval, message ->
            //We cannot use post() here since it's possible that the calendar is still not
            //attached to the fragment
            UIUtils.runOnMainThread {
                lessons.filterLessons()

                currentlyShownLessonsSet.mergeWith(lessons)

                addEnabledInterval(interval)
                addEventsFromLessonsSet(lessons)

                onLoadingOperationNotify.dispatch(message)
            }
        }

        loader!!.onPartiallyCachedResultsFetched.connect { lessons ->
            //We cannot use post() here since it's possible that the calendar is still not
            //attached to the fragment
            UIUtils.runOnMainThread {
                lessons.filterLessons()

                currentlyShownLessonsSet.mergeWith(lessons)

                val cachedIntervals = lessons.cachedWeekIntervals
                addEnabledIntervals(cachedIntervals)

                addEventsFromLessonsSet(lessons)
            }
        }
    }

    private fun addEnabledIntervals(intervals: ArrayList<WeekInterval>) {
        for (interval in intervals) {
            addEnabledInterval(interval)
        }
    }

    fun loadEventsNearToday() = loader!!.loadEventsNearDay(CalendarUtils.debuggableToday)

    private fun addEventsFromLessonsSet(lessons: LessonsSet) =
            addEvents(makeEventsFromLessonsSet(lessons))

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean =
            date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                    date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                    date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)

    private fun makeEventsFromLessonsSet(lessonsSet: LessonsSet): List<WeekViewEvent> =
            lessonsSet.scheduledLessons.mapTo(ArrayList(), {
        LessonToEventAdapter(it.value)
    })

    //        val events = ArrayList<LessonToEventAdapter>()
    //        for ((_, lessonSchedule) in lessonsSet.scheduledLessons) {
    //            events.add(LessonToEventAdapter(lessonSchedule))
    //        }
    //
    //        return events

    override fun onFirstVisibleDayChanged(newFirst: Calendar, oldFirst: Calendar) {
        if (isInEditMode) {
            return
        }

        val scrollDirection = if (newFirst.after(oldFirst)) ScrollDirection.RIGHT else ScrollDirection.LEFT

        val disabledDays = getDisabledDaysVisibleFromDay(newFirst)
        if (disabledDays.isFirstDayDisabled) {
            loader!!.loadDaysIfNeeded(disabledDays.firstVisibleWeek!!, scrollDirection)
        }

        if (disabledDays.isLastDayDisabled) {
            loader!!.loadDaysIfNeeded(disabledDays.lastVisibleWeek!!, scrollDirection)
        }
    }

    val currentLessonTypes: Collection<LessonType>
        get() = currentlyShownLessonsSet.lessonTypes.values

    fun notifyLessonTypeVisibilityChanged() {
        clear()

        loader!!.loadEventsNearDay(firstVisibleDay)
    }

    fun rotateNumOfDaysShown(): Int {
        val numOfDaysToShow = getNextNumberOfDaysToShow(numberOfVisibleDays)
        prepareForNumberOfVisibleDays(numOfDaysToShow)

        return numOfDaysToShow
    }

    fun prepareForNumberOfVisibleDays(numOfDaysToShow: Int) {
        numberOfVisibleDays = numOfDaysToShow
        dateTimeInterpreter = appropriateDateTimeInterpreter
        goToDate(firstVisibleDay) //Fixes #47
        invalidate()
    }

    /**
     * Note: do not call this method directly, but use
     * [CourseTimesCalendar.prepareForNumberOfVisibleDays] instead
     */
    override var numberOfVisibleDays: Int
        get() = super.numberOfVisibleDays
        set(numberOfVisibleDays) {
            super.numberOfVisibleDays = numberOfVisibleDays
        }

    private fun getNextNumberOfDaysToShow(numberOfVisibleDays: Int): Int =
        when (numberOfVisibleDays) {
            1 -> 2
            3 -> 7
            7 -> 1
        /*2*/ else -> 3
        }

    class LessonToEventAdapter(val lesson: LessonSchedule) : WeekViewEvent(
            lesson.id,
            lesson.fullDescription,
            lesson.startCal,
            lesson.endCal) {

        init {
            this.color = lesson.color
        }

    }

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN)
        private val DATE_FORMAT_DEBUG = SimpleDateFormat("(w) EEEE d MMMM", Locale.ITALIAN)

        private val DATE_FORMAT_MEDIUM = SimpleDateFormat("EE dd/MM", Locale.ITALIAN)
        private val DATE_FORMAT_MEDIUM_DEBUG = SimpleDateFormat("(w)EE dd/MM", Locale.ITALIAN)

        private val DATE_FORMAT_SHORT = SimpleDateFormat("dd/MM", Locale.ITALIAN)
        private val DATE_FORMAT_SHORT_DEBUG = SimpleDateFormat("(w)dd/MM", Locale.ITALIAN)

        private val DATE_FORMAT_SHORT_ONLY_DAY = SimpleDateFormat("EE", Locale.ITALIAN)

        private val FORMAT_ONLY_DAY = SimpleDateFormat("EEEE", Locale.ITALIAN)
    }

}
