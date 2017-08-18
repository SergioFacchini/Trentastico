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
import com.geridea.trentastico.gui.adapters.PartitioningsAdapter
import com.geridea.trentastico.gui.views.CourseTimesCalendar
import com.geridea.trentastico.gui.views.requestloader.RequestLoaderView
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.time.CalendarUtils
import com.threerings.signals.Listener1
import com.threerings.signals.Signal1
import java.util.*

class CalendarFragment : FragmentWithMenuItems() {

    @BindView(R.id.calendar)   lateinit internal var calendar: CourseTimesCalendar
    @BindView(R.id.loaderView) lateinit internal var loaderView: RequestLoaderView

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_calendar, container, false)
        ButterKnife.bind(this, view)

        //Binding calendar
        calendar.prepareForNumberOfVisibleDays(AppPreferences.calendarNumOfDaysToShow)
        calendar.setEventsTextSize(AppPreferences.calendarFontSize)
        calendar.goToDate(CalendarUtils.debuggableToday)
        calendar.onLoadingOperationNotify.connect { operation -> loaderView.processMessage(operation) }

        calendar.loadEventsNearToday()

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

        private val lessonTypes: Collection<LessonType>
        private val courseAdapter: CourseFilterAdapter

        init {

            //Inflating the view
            val view = Views.inflate<View>(context, R.layout.dialog_filter_courses)

            ButterKnife.bind(this, view)

            lessonTypes = calendar.currentLessonTypes

            if (lessonTypes.isEmpty()) {
                yesCoursesText.visibility = GONE
            } else {
                noCoursesText.visibility = GONE
            }


            courseAdapter = CourseFilterAdapter(context, lessonTypes)
            courseAdapter.onLessonTypeVisibilityChanged.connect { lesson ->
                AppPreferences.lessonTypesIdsToHide = calculateLessonTypesToHide()
                if (lesson.applyVisibilityToPartitionings()) {
                    AppPreferences.updatePartitioningsToHide(lesson)
                    courseAdapter.notifyDataSetChanged() //Updating %d of %d shown

                    //Updating notifications
                    NextLessonNotificationService.createIntent(context, NLNStarter.FILTERS_CHANGED)
                }

                calendar.notifyLessonTypeVisibilityChanged()
            }
            courseAdapter.onConfigurePartitioningButtonClicked.connect { lessonType ->
                val filterPartitionings = FilterPartitioningsDialog(context, lessonType)
                filterPartitionings.onPartitioningVisibilityChanged.connect { affectedLessonType ->
                    if (affectedLessonType.hasAllPartitioningsInvisible()) {
                        affectedLessonType.isVisible = false
                        AppPreferences.lessonTypesIdsToHide = calculateLessonTypesToHide()
                    } else if (affectedLessonType.hasAtLeastOnePartitioningVisible()) {
                        affectedLessonType.isVisible = true
                        AppPreferences.lessonTypesIdsToHide = calculateLessonTypesToHide()
                    }

                    courseAdapter.notifyDataSetChanged() //Updating %d of %d shown
                    calendar.notifyLessonTypeVisibilityChanged()
                }
                filterPartitionings.show()
            }
            coursesListView.adapter = courseAdapter

            setView(view)
        }

        private fun calculateLessonTypesToHide(): ArrayList<Long> {
            val lessonTypesToHideIds = ArrayList<Long>()
            for (lessonType in lessonTypes) {
                if (!lessonType.isVisible) {
                    lessonTypesToHideIds.add(lessonType.id.toLong())
                }
            }

            return lessonTypesToHideIds
        }

        @OnClick(R.id.dismiss_button)
        fun onDoFilterButtonClicked() = dismiss()

    }

    internal inner class FilterPartitioningsDialog(
            context: Context,
            lessonType: LessonType) : AlertDialog(context) {

        val onPartitioningVisibilityChanged = Signal1<LessonType>()

        @BindView(R.id.partitionings_list)
        lateinit var partitioningsList: ListView
        @BindView(R.id.introduction_text)
        lateinit var introductionText: TextView

        init {

            val view = Views.inflate<View>(context, R.layout.dialog_filter_partition)
            ButterKnife.bind(this, view)

            calculateIntroductionText(lessonType)
            bindAdapter(context, lessonType)

            setView(view)
        }

        private fun bindAdapter(context: Context, lessonType: LessonType) {
            val adapter = PartitioningsAdapter(context, lessonType.partitioning)
            adapter.onPartitioningVisibilityChanged.connect(Listener1 {
                AppPreferences.updatePartitioningsToHide(lessonType)
                calendar.notifyLessonTypeVisibilityChanged()

                onPartitioningVisibilityChanged.dispatch(lessonType)
            })
            partitioningsList.adapter = adapter
        }

        private fun calculateIntroductionText(lessonType: LessonType) {
            val introductionString = String.format(Locale.CANADA,
                    "Gli studenti del corso \"%s\" sono divisi in %d gruppi.\nQui sotto puoi togliere la " + "spunta dai gruppi di cui non fai parte per nascondere il relativo orario.",
                    lessonType.name, lessonType.partitioning.partitioningCasesSize
            )
            introductionText.text = introductionString
        }

        @OnClick(R.id.dismiss_button)
        fun onDismissButtonPressed() = dismiss()

    }


}
