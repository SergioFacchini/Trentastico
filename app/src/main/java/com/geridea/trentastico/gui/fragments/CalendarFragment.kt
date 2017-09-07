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
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.gui.adapters.CourseFilterAdapter
import com.geridea.trentastico.gui.views.CourseTimesCalendar
import com.geridea.trentastico.gui.views.requestloader.RequestLoaderView
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.model.Teacher
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.orIfEmpty
import com.geridea.trentastico.utils.setTextOrHideIfEmpty
import com.geridea.trentastico.utils.time.CalendarUtils
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.dialog_calendar_event.view.*
import kotlinx.android.synthetic.main.itm_teacher.view.*
import java.util.*

class CalendarFragment : FragmentWithMenuItems() {

    @BindView(R.id.calendar)   lateinit internal var calendar: CourseTimesCalendar
    @BindView(R.id.loaderView) lateinit internal var loaderView: RequestLoaderView

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        ButterKnife.bind(this, view)

        //Binding calendar
        calendar.prepareForNumberOfVisibleDays(AppPreferences.calendarNumOfDaysToShow)
        calendar.setEventsTextSize(AppPreferences.calendarFontSize)
        calendar.goToDate(CalendarUtils.debuggableToday)
        calendar.onLoadingOperationNotify.connect { operation -> loaderView.processMessage(operation) }
        calendar.onEventClicked.connect { clickedEvent, lessonType ->
            showClickedEventPopup(clickedEvent, lessonType)
        }

        calendar.loadEvents()

        return view
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = intArrayOf(R.id.menu_filter, R.id.menu_change_view)

    override fun bindMenuItem(item: MenuItem) {
        if (item.itemId == R.id.menu_filter) {
            item.setOnMenuItemClickListener {
                FilterCoursesDialog(activity).show()
                true
            }
        } else if (item.itemId == R.id.menu_change_view) {
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

    @DrawableRes
    private fun getChangeViewMenuIcon(numOfDays: Int): Int =
        when (numOfDays) {
            2    -> R.drawable.ic_calendar_show_2
            3    -> R.drawable.ic_calendar_show_3
            7    -> R.drawable.ic_calendar_show_7
            else -> R.drawable.ic_calendar_show_1
        }

    private fun showClickedEventPopup( clickedEvent: LessonSchedule, lessonType: LessonTypeNew){
        if (context != null) {
            ShowLessonDetailsDialog(context, clickedEvent, lessonType).show()
        }
    }

    internal inner class FilterCoursesDialog(context: Context) : AlertDialog(context) {

        @BindView(R.id.coursesListView)
        lateinit var coursesListView: ListView

        @BindView(R.id.noCoursesText)
        lateinit var noCoursesText: TextView

        @BindView(R.id.yesCoursesText)
        lateinit var yesCoursesText: TextView

        @BindView(R.id.extra_course)
        lateinit var extraCourseDescription: View

        private val lessonTypes: Collection<LessonTypeNew>

        private var wasSomeVisibilityChanged = false

        init {
            lessonTypes = calendar.currentLessonTypes

            //Inflating the view
            val view = Views.inflate<View>(context, R.layout.dialog_filter_courses)
            ButterKnife.bind(this, view)
            setView(view)

            //Setting up the view
            (if (lessonTypes.isEmpty()) yesCoursesText else noCoursesText).visibility = GONE

            //Setting up adapter
            val courseAdapter = CourseFilterAdapter(context, lessonTypes)
            courseAdapter.onLessonTypeVisibilityChanged.connect { lesson ->
                AppPreferences.lessonTypesToHideIds = calculateLessonTypesToHide()
                wasSomeVisibilityChanged = true
            }
            coursesListView.adapter = courseAdapter

            //Show extra lessons description if there is any in the list
            if (lessonTypes.none { AppPreferences.extraCourses.hasCourseWithId(it.id) }) {
                extraCourseDescription.visibility = View.GONE
            }

            //Update calendar on visibility changes
            setOnDismissListener {
                if (wasSomeVisibilityChanged) {
                    calendar.notifyEventsChanged()
                }
            }
        }

        private fun calculateLessonTypesToHide(): ArrayList<String> =
            lessonTypes
                .filterNot { it.isVisible }
                .mapTo(ArrayList()) { it.id }

        @OnClick(R.id.dismiss_button)
        fun onDoFilterButtonClicked() = dismiss()

    }

    internal inner class ShowLessonDetailsDialog(
            context: Context,
            event: LessonSchedule,
            lessonType: LessonTypeNew) : AlertDialog(context) {

        init {
            val view = Views.inflate<View>(context, R.layout.dialog_calendar_event)
            setView(view)

            view.lesson_title  .text = event.subject
            view.room_name     .text = event.calculateCompleteRoomNames("\n")
            view.starting      .text = CalendarUtils.formatRangeComplete(event.startsAt, event.endsAt)
            view.kind_of_lesson.text = "Tipologia: "+lessonType.kindOfLesson

            view.partitioning_name.setTextOrHideIfEmpty(event.partitioningName)

            val teachers = lessonType.teachers.orIfEmpty(Teacher.PLACEHOLDER_TEACHER_LIST)
            view.teachers_names.adapter = TeachersAdapter(context, teachers)
        }

    }

}

class TeachersAdapter(context: Context, teachers: List<Teacher>) : ItemsAdapter<Teacher>(context) {

    init {
        itemsList = teachers
    }

    override fun bindView(item: Teacher, pos: Int, convertView: View) {
        convertView.teacher_name.text = item.name

        Picasso.with(context)
                .load(item.teacherPhotoUrl)
                .placeholder(R.drawable.teacher_no_photo)
                .into(convertView.photo)

        convertView.setOnClickListener {
            when {
                item.id == "181898" -> Toast.makeText(context, "Violette!", Toast.LENGTH_SHORT).show()
                item.id == "004437" -> Toast.makeText(context, "Peanuts!",  Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun createView(item: Teacher?, pos: Int, parent: ViewGroup?, inflater: LayoutInflater): View =
            inflater.inflate(R.layout.itm_teacher, parent, false)

}
