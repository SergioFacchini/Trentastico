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
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.gui.adapters.CourseFilterAdapter
import com.geridea.trentastico.gui.views.CourseTimesCalendar
import com.geridea.trentastico.gui.views.requestloader.RequestLoaderView
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarUtils
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
                    calendar.notifyLessonTypeVisibilityChanged()
                }
            }
        }

        private fun calculateLessonTypesToHide(): ArrayList<String> {
            return lessonTypes
                    .filterNot { it.isVisible }
                    .mapTo(ArrayList()) { it.id }
        }

        @OnClick(R.id.dismiss_button)
        fun onDoFilterButtonClicked() = dismiss()

    }

}
