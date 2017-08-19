package com.geridea.trentastico.gui.fragments

/*
 * Created with ♥ by Slava on 19/03/2017.
 */

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.gui.views.CourseSelectorView
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.LessonsUpdaterService
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.threerings.signals.Signal1

class SettingsFragment : FragmentWithMenuItems() {
    /**
     * Prevents listeners from triggering unnecessarily.
     */
    var isLoading = true

    //Calendar
    @BindView(R.id.font_size_seek_bar) lateinit var fontSizeSeekBar: SeekBar
    @BindView(R.id.font_preview) lateinit var fontSizePreview: TextView

    //Study course
    @BindView(R.id.current_study_course) lateinit var currentStudyCourse: TextView

    //Lessons updates
    @BindView(R.id.search_for_lesson_changes) lateinit var searchForLessonChanges: Switch
    @BindView(R.id.lesson_change_show_notification) lateinit var shownNotificationOnLessonChanges: Switch

    //Next lesson notification
    @BindView(R.id.show_next_lesson_notification) lateinit var showNextLessonNotification: Switch
    @BindView(R.id.make_notifications_fixed) lateinit var makeNotificationFixedSwitch: Switch


    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        isLoading = true

        val view = inflater!!.inflate(R.layout.fragment_settings, container, false)
        ButterKnife.bind(this, view)

        //Calendar
        fontSizeSeekBar.progress = AppPreferences.calendarFontSize - MIN_CALENDAR_FONT_SIZE
        updateCalendarFontPreview(AppPreferences.calendarFontSize)

        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean) = updateCalendarFontPreview(MIN_CALENDAR_FONT_SIZE + progress)

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                AppPreferences.calendarFontSize = seekBar.progress + MIN_CALENDAR_FONT_SIZE
            }
        })

        //Study courses
        val studyCourse = AppPreferences.studyCourse
        currentStudyCourse.text = studyCourse.generateFullDescription()

        //Lesson changes
        searchForLessonChanges.isChecked = AppPreferences.isSearchForLessonChangesEnabled
        shownNotificationOnLessonChanges.isChecked = AppPreferences.isNotificationForLessonChangesEnabled

        //Next lesson notification
        showNextLessonNotification.isChecked = AppPreferences.areNextLessonNotificationsEnabled()
        makeNotificationFixedSwitch.isChecked = AppPreferences.areNextLessonNotificationsFixed()

        isLoading = false

        return view
    }

    private fun updateCalendarFontPreview(fontSize: Int) = fontSizePreview.setTextSize(COMPLEX_UNIT_SP, fontSize.toFloat())

    ///////////////////////////
    ////LESSONS UPDATES
    ///////////////////////////

    @OnClick(R.id.change_study_course_button)
    internal fun onChangeStudyCourseButtonPressed() {
        val dialog = ChangeStudyCourseDialog(activity)
        dialog.onChoiceMade.connect { studyCourse -> currentStudyCourse.text = studyCourse.generateFullDescription() }
        dialog.show()
    }

    @OnCheckedChanged(R.id.search_for_lesson_changes)
    internal fun onSearchForLessonsSwitchChanged(enabled: Boolean) {
        if (isLoading) {
            return
        }

        AppPreferences.isSearchForLessonChangesEnabled = enabled
        shownNotificationOnLessonChanges.isEnabled = enabled

        if (enabled) {
            activity.startService(
                    LessonsUpdaterService.createIntent(activity, LessonsUpdaterService.STARTER_SETTING_CHANGED)
            )
        } else {
            LessonsUpdaterService.cancelSchedules(activity, LessonsUpdaterService.STARTER_SETTING_CHANGED)
        }
    }

    @OnCheckedChanged(R.id.search_for_lesson_changes)
    internal fun onShowLessonChangeNotificationSwitchChanged(checked: Boolean) {
        if (isLoading) {
            return
        }

        AppPreferences.isNotificationForLessonChangesEnabled = checked
    }

    ///////////////////////////
    ////NEXT LESSON NOTIFICATION
    ///////////////////////////

    @OnCheckedChanged(R.id.show_next_lesson_notification)
    internal fun onShowNextLessonNotificationSwitchChanged(checked: Boolean) {
        if (isLoading) {
            return
        }

        AppPreferences.setNextLessonNotificationsEnabled(checked)
        makeNotificationFixedSwitch.isEnabled = checked

        if (checked) {
            startNextLessonNotificationService()
        } else {
            NextLessonNotificationService.clearNotifications(activity)
        }
    }

    private fun startNextLessonNotificationService() {
        activity.startService(NextLessonNotificationService.createIntent(
                activity, NLNStarter.NOTIFICATIONS_SWITCHED_ON)
        )
    }

    @OnCheckedChanged(R.id.make_notifications_fixed)
    internal fun onMakeNotificationsFixedSwitchChanged(checked: Boolean) {
        if (isLoading) {
            return
        }

        AppPreferences.setNextLessonNotificationsFixed(checked)

        //If we have any notification, we have to update them:
        startNextLessonNotificationService()
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //Does not uses menus, nothing to bind!
            Unit


    inner class ChangeStudyCourseDialog(context: Context) : AlertDialog(context) {

        /**
         * Dispatched when the user changed or did not change the study course and just pressed OK.
         */
        val onChoiceMade = Signal1<StudyCourse>()

        @BindView(R.id.course_selector) lateinit var courseSelector: CourseSelectorView
        @BindView(R.id.change_button)   lateinit var changeButton: Button

        init {
            val view = Views.inflate<View>(context, R.layout.dialog_change_study_course)
            ButterKnife.bind(this, view)

            courseSelector.selectStudyCourse(AppPreferences.studyCourse)

            changeButton.visibility = View.GONE

            courseSelector.onCoursesLoaded.connect {
                changeButton.visibility = View.VISIBLE
            }
            courseSelector.loadCourses()

            setView(view)
        }

        @OnClick(R.id.change_button)
        internal fun onChangeStudyCourseButtonClicked() {
            val selectedCourse = courseSelector.buildStudyCourse()
            if (AppPreferences.studyCourse == selectedCourse) {
                //We just clicked ok without changing our course...
                onChoiceMade.dispatch(selectedCourse)

                dismiss()
            } else {
                clearFilters()
                clearCache(selectedCourse)
                removeOverlappingExtraCourses(selectedCourse)

                AppPreferences.studyCourse = selectedCourse

                dealWithNextLessonNotifications()
                onChoiceMade.dispatch(selectedCourse)

                dismiss()
            }

        }

        private fun dealWithNextLessonNotifications() {
            NextLessonNotificationService.clearNotifications(context)

            //We need to show the next lesson notification for the new course
            activity.startService(NextLessonNotificationService.createIntent(
                    context, NLNStarter.STUDY_COURSE_CHANGE
            ))
        }

        private fun removeOverlappingExtraCourses(selectedCourse: StudyCourse)
                = AppPreferences.removeExtraCoursesOfCourse(selectedCourse)

        private fun clearFilters() {
            AppPreferences.removeAllHiddenCourses() //No longer need them
            AppPreferences.removeAllHiddenPartitionings() //No longer need them
        }

        private fun clearCache(selectedCourse: StudyCourse) {
            //We changed our course, let's wipe out all the cache!
            Networker.purgeStudyCourseCache()

            //If we've just selected a course that we already had in our extra course, we need to
            //delete that course from cache
            val overlappingExtraCourses = AppPreferences.getExtraCoursesOfCourse(selectedCourse)
            for (overlappingExtraCourse in overlappingExtraCourses) {
                Networker.removeExtraCoursesWithLessonType(overlappingExtraCourse.lessonTypeId)
            }
        }

    }

    companion object {

        val MIN_CALENDAR_FONT_SIZE = 7
    }

}
