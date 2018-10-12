package com.geridea.trentastico.gui.fragments

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.gui.adapters.CourseFilterAdapter
import com.geridea.trentastico.gui.views.CourseTimesCalendar
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.NO_TEACHER_ASSIGNED_DEFAULT_TEXT
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.orIfEmpty
import com.geridea.trentastico.utils.setTextOrHideIfEmpty
import com.geridea.trentastico.utils.time.CalendarUtils
import kotlinx.android.synthetic.main.dialog_calendar_event.view.*
import kotlinx.android.synthetic.main.dialog_filter_courses.view.*
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.itm_teacher.view.*
import java.util.*

class CalendarFragment : FragmentWithMenuItems() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //Binding calendar
        if (AppPreferences.lastZoom != 0) {
            calendar.zoom = AppPreferences.lastZoom
        }

        calendar.prepareForNumberOfVisibleDays(AppPreferences.calendarNumOfDaysToShow, jumpToFirstVisibleDay = false)
        calendar.setEventsTextSize(AppPreferences.calendarFontSize)
        calendar.goToDate(CalendarUtils.debuggableToday)
        calendar.onEventClicked.connect { clickedEvent, lessonType ->
            showClickedEventPopup(clickedEvent, lessonType)
        }

        val today = CalendarUtils.today()
        calendar.onFirstVisibleDayChanged.connect { newDay ->
            val isCalendarOnToday = CalendarUtils.isSameDay(newDay, today)
            if (isCalendarOnToday) {
                todayFab.hide()
            } else
                todayFab.show()
        }

        calendar.loadEvents()

        //Binding other controls
        todayFab.setOnClickListener { calendar.goToDate(Calendar.getInstance()) }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? =
            inflater.inflate(R.layout.fragment_calendar, container, false)

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = intArrayOf(R.id.menu_refresh, R.id.menu_filter, R.id.menu_change_view)

    override fun bindMenuItem(item: MenuItem) {
        when {
            item.itemId == R.id.menu_refresh     -> item.setOnMenuItemClickListener {
                Networker.obliterateCache()
                Toast.makeText(context, "Sto ricaricando i tuoi orari...", Toast.LENGTH_LONG).show()
                goToCalendarFragment()
                true
            }
            item.itemId == R.id.menu_filter      -> item.setOnMenuItemClickListener {
                FilterCoursesDialog(requireContext(), calendar).show()
                true
            }
            item.itemId == R.id.menu_change_view -> {
                //Note we cannot call here calendar.getNumberOfVisibleDays() because this might have
                //been called before onCreate
                item.setIcon(getChangeViewMenuIcon(AppPreferences.calendarNumOfDaysToShow))
                item.setOnMenuItemClickListener { i ->
                    val numOfDays = calendar.rotateNumOfDaysShown()
                    i.setIcon(getChangeViewMenuIcon(numOfDays))
                    AppPreferences.calendarNumOfDaysToShow = numOfDays

                    true
                }
            }
        }
    }

    @DrawableRes
    private fun getChangeViewMenuIcon(numOfDays: Int): Int =
        when (numOfDays) {
            2    -> R.drawable.ic_calendar_show_2
            3    -> R.drawable.ic_calendar_show_3
            5    -> R.drawable.ic_calendar_show_7
            else -> R.drawable.ic_calendar_show_1
        }

    private fun showClickedEventPopup( clickedEvent: LessonSchedule, lessonType: LessonType){
        if (context != null) {
            ShowLessonDetailsDialog(requireContext(), clickedEvent, lessonType).show()
        }
    }

    internal inner class FilterCoursesDialog(context: Context, calendar: CourseTimesCalendar) : AlertDialog(context) {

        private val lessonTypes: Collection<LessonType>

        private var wasSomeVisibilityChanged = false

        init {
            lessonTypes = calendar.currentLessonTypes

            //Inflating the view
            val view = Views.inflate<View>(context, R.layout.dialog_filter_courses)

            //Setting up the view
            (if (lessonTypes.isEmpty()) view.yesCoursesText else view.noCoursesText).visibility = GONE

            //Setting up adapter
            val courseAdapter = CourseFilterAdapter(context, lessonTypes)
            courseAdapter.onLessonTypeVisibilityChanged.connect { _ ->
                AppPreferences.lessonTypesToHideIds = calculateLessonTypesToHide()
                wasSomeVisibilityChanged = true
            }
            view.coursesListView.adapter = courseAdapter

            //Show extra lessons description if there is any in the list
            if (lessonTypes.none { AppPreferences.extraCourses.hasCourseWithId(it.id) }) {
                view.extraCourseDescription.visibility = View.GONE
            }

            view.dismiss_button.setOnClickListener {
                dismiss()
            }

            //Update calendar on visibility changes
            setOnDismissListener {
                if (wasSomeVisibilityChanged) {
                    calendar.notifyEventsChanged()
                }
            }

            setView(view)
        }

        private fun calculateLessonTypesToHide(): ArrayList<String> =
            lessonTypes
                .filterNot { it.isVisible }
                .mapTo(ArrayList()) { it.id }

    }

    internal inner class ShowLessonDetailsDialog(
            context: Context,
            event: LessonSchedule,
            lessonType: LessonType) : AlertDialog(context) {

        init {
            val view = Views.inflate<View>(context, R.layout.dialog_calendar_event)
            setView(view)

            view.lesson_title  .text = event.subject
            view.room_name     .text = event.calculateCompleteRoomNames("\n")
            view.starting      .text = CalendarUtils.formatRangeComplete(event.startsAt, event.endsAt)
            view.kind_of_lesson.text = "Tipologia: "+lessonType.kindOfLesson

            view.partitioningName.setTextOrHideIfEmpty(event.partitioningName)

            val teachers = lessonType.teachers.orIfEmpty(listOf(NO_TEACHER_ASSIGNED_DEFAULT_TEXT))
            view.teachers_names.adapter = TeachersAdapter(context, teachers)
        }

    }

    override fun onStop() {
        AppPreferences.lastZoom = calendar.zoom

        super.onStop()
    }

}

class TeachersAdapter(context: Context, teachers: List<String>) : ItemsAdapter<String>(context) {

    init {
        itemsList = teachers
    }

    override fun bindView(item: String, pos: Int, convertView: View) {
        convertView.teacherName.text = item
    }

    override fun createView(item: String?, pos: Int, parent: ViewGroup?, inflater: LayoutInflater): View =
        inflater.inflate(R.layout.itm_teacher, parent, false)

}
