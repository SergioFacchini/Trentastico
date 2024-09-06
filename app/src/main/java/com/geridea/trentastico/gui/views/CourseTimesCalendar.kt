package com.geridea.trentastico.gui.views


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekViewEvent
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.network.LessonsLoader
import com.geridea.trentastico.utils.ColorDispenser
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import com.geridea.trentastico.utils.time.DayInterval
import com.threerings.signals.Signal1
import com.threerings.signals.Signal2
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CourseTimesCalendar : CustomWeekView, CustomWeekView.EventClickListener {

    //Signals
    /**
     * Dispatched when the user clicks a specific event
     */
    val onEventClicked = Signal2<LessonSchedule, LessonType>()

    /**
     * Dispatched when something worth of note is happening in the calendar. For instance starting
     * or finishing fetching lessons from network.<br></br>
     * WARNING: may be called on a not-UI thread!
     */
    val onLoadingOperationNotify = Signal1<ILoadingMessage>()

    val currentLessonTypes: MutableSet<LessonType> = mutableSetOf()

    //Data
    private lateinit var loader: LessonsLoader

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(
            context: Context,
            attrs: AttributeSet,
            defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        prepareLoader()
        dateTimeInterpreter = getDateInterpreterForNumberOfDays(numberOfVisibleDays)
        eventClickListener = this
    }

    private fun getDateInterpreterForNumberOfDays(numberOfVisibleDays: Int): DateTimeInterpreter =
        when {
            numberOfVisibleDays <= 2 -> object : DateTimeInterpreter {
                override fun interpretDate(date: Calendar): String {
                    val formattedDay = FORMAT_ONLY_DAY.format(date.time).toUpperCase()

                    val day = CalendarUtils.debuggableToday
                    if (isSameDay(day, date)) return "$formattedDay\nOggi"

                    day.add(Calendar.DAY_OF_MONTH, +1)
                    return if (isSameDay(day, date)) "$formattedDay\nDomani"
                    else formattedDay+"\n"+ DAY_AND_MONTH_FORMAT.format(date.time)
                }

                override fun interpretTime(hour: Int): String = interpretHours(hour)
            }

            numberOfVisibleDays < 5 -> object : DateTimeInterpreter {
                override fun interpretDate(date: Calendar): String {
                    val dayName = FORMAT_ONLY_DAY.format(date.time).toUpperCase()

                    val day = CalendarUtils.debuggableToday
                    if (isSameDay(day, date)) return "$dayName\nOggi"

                    day.add(Calendar.DAY_OF_MONTH, +1)
                    if (isSameDay(day, date)) return "$dayName\nDomani"

                    return dayName+"\n"+DAY_AND_MONTH_FORMAT.format(date.time)
                }

                override fun interpretTime(hour: Int): String = interpretHours(hour)
            }
            else -> // > 6
                object : DateTimeInterpreter {
                    override fun interpretDate(date: Calendar): String {
                        val dayName     = DATE_FORMAT_SHORT_ONLY_DAY.format(date.time).toUpperCase()
                        val dayAndMonth = DAY_AND_MONTH_SHORT_FORMAT.format(date.time)

                        return dayName+"\n"+dayAndMonth
                    }

                    override fun interpretTime(hour: Int): String = interpretHours(hour)
                }
        }


    private fun interpretHours(hour: Int): String = hour.toString()

    private fun prepareLoader() {
        loader = LessonsLoader()
        loader.onLoadingMessageDispatched  .connect { message -> onLoadingOperationNotify.dispatch(message) }
        loader.onLoadingOperationSuccessful.connect { scheduledLessons, lessonTypes, message ->

            //We cannot use post() here since it's possible that the calendar is still not
            //attached to the fragment
            UIUtils.runOnMainThread {
                currentLessonTypes.addAll(lessonTypes)

                if (scheduledLessons.isNotEmpty()) {
                    //If empty, the covered interval calculation will fail
                    addEnabledInterval(calculateCoveredInterval(scheduledLessons))
                    addLessons(scheduledLessons)

                    if(!loader.hasAnythingBeingLoaded){
                        goToFirstEnabledDayIfNeeded()
                    }
                }

                onLoadingOperationNotify.dispatch(message)
            }
        }
    }

    private fun goToFirstEnabledDayIfNeeded() {
        //We send the user to the first enabled day only in case the lessons of the user still
        // didn't start
        val firstEnabledDay = calculateFirstEnabledDayMillis()
        if(CalendarUtils.debuggableToday.timeInMillis < firstEnabledDay.millis){
            val calendar = CalendarUtils.clearCalendar
            calendar.timeInMillis = firstEnabledDay.millis
            goToDate(calendar)
        }
    }

    /**
     * @return the [DayInterval] that starts with the earliest lesson and ends with the latest one
     */
    private fun calculateCoveredInterval(scheduledLessons: List<LessonSchedule>): DayInterval {
        val minTime = scheduledLessons.minOfOrNull { it.startsAt }
        val maxTime = scheduledLessons.maxOfOrNull { it.endsAt }

        return DayInterval(
            CalendarUtils.getCalendarWithMillis(minTime!!),
            CalendarUtils.getCalendarWithMillis(maxTime!!)
        )
    }

    fun loadEvents() = loader.loadAndAddLessons()

    private fun addLessons(lessons: List<LessonSchedule>) =
            addEvents(makeEventsFromLessons(lessons))

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean =
            date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                    date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                    date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)

    private fun makeEventsFromLessons(lessons: List<LessonSchedule>): List<WeekViewEvent> {
        return lessons.mapTo(ArrayList()) {
            LessonToEventAdapter(it, ColorDispenser.getColor(it.lessonTypeId))
        }
    }

    fun notifyEventsChanged() {
        clear()

        loader.loadAndAddLessons()
    }

    fun rotateNumOfDaysShown(): Int {
        val numOfDaysToShow = getNextNumberOfDaysToShow(numberOfVisibleDays)
        prepareForNumberOfVisibleDays(numOfDaysToShow)

        return numOfDaysToShow
    }

    fun prepareForNumberOfVisibleDays(numOfDaysToShow: Int, jumpToFirstVisibleDay: Boolean = true) {
        numberOfVisibleDays = numOfDaysToShow
        dateTimeInterpreter = getDateInterpreterForNumberOfDays(numberOfVisibleDays)

        if(jumpToFirstVisibleDay){
            goToDate(firstVisibleDay) //Fixes #47
        }
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
            3 -> 5
            5 -> 1
        /*2*/ else -> 3
        }

    override fun onEventClick(clickedEvent: LessonToEventAdapter, eventRect: RectF?) {
        onEventClicked.dispatch(clickedEvent.lesson, getLessonTypeOfEvent(clickedEvent))
    }

    private fun getLessonTypeOfEvent(event: LessonToEventAdapter) =
            currentLessonTypes.first { it.id == event.lesson.lessonTypeId }

    class LessonToEventAdapter(val lesson: LessonSchedule, color: Int) : WeekViewEvent(
            nextId, lesson.eventDescription, lesson.startCal, lesson.endCal
    ) {

        init {
            this.color = color
        }

        companion object {
            var nextId = 1L
                get() = field++
                private set
        }

    }

    companion object {
        private val DAY_AND_MONTH_FORMAT       = SimpleDateFormat("d MMMM", Locale.ITALIAN)
        private val DAY_AND_MONTH_SHORT_FORMAT = SimpleDateFormat("d MMM",  Locale.ITALIAN)
        private val DATE_FORMAT_SHORT_ONLY_DAY = SimpleDateFormat("EE",     Locale.ITALIAN)
        private val FORMAT_ONLY_DAY            = SimpleDateFormat("EEEE",   Locale.ITALIAN)
    }

}
