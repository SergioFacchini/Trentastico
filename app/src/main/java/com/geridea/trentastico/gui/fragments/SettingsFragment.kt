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
import android.widget.SeekBar
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ColorDispenser
import com.threerings.signals.Signal1
import kotlinx.android.synthetic.main.dialog_change_study_course.*
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : FragmentWithMenuItems() {
    /**
     * Prevents listeners from triggering unnecessarily.
     */
    private var isLoading = true

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        //Lesson updates
        changeStudyCourseButton.setOnClickListener {
            val dialog = ChangeStudyCourseDialog(requireContext())
            dialog.onChoiceMade.connect {
                studyCourse -> currentStudyCourse.text = studyCourse.generateFullDescription()
            }
            dialog.show()
        }


        //Study courses
        val studyCourse = AppPreferences.studyCourse
        currentStudyCourse.text = studyCourse.generateFullDescription()

        //Lesson changes
        searchForLessonChanges.isChecked           = AppPreferences.isSearchForLessonChangesEnabled
        shownNotificationOnLessonChanges.isChecked = AppPreferences.isNotificationForLessonChangesEnabled

        searchForLessonChanges.setOnCheckedChangeListener { _, checked ->
            if (isLoading) {
                return@setOnCheckedChangeListener
            }

            AppPreferences.nextLessonNotificationsFixed = checked

            //If we have any notification, we have to update them:
            startNextLessonNotificationService()
        }

        shownNotificationOnLessonChanges.setOnCheckedChangeListener { _, checked ->
            if(!isLoading) {
                AppPreferences.isNotificationForLessonChangesEnabled = checked
            }
        }

        //Next lesson notification
        showNextLessonNotification .isChecked  = AppPreferences.nextLessonNotificationsEnabled
        makeNotificationFixedSwitch.isChecked = AppPreferences.nextLessonNotificationsFixed

        showNextLessonNotification.setOnCheckedChangeListener { _, checked ->
            if (isLoading) {
                return@setOnCheckedChangeListener
            }

            AppPreferences.nextLessonNotificationsEnabled = checked
            makeNotificationFixedSwitch.isEnabled = checked

            if (checked) {
                startNextLessonNotificationService()
            } else {
                NextLessonNotificationService.clearNotifications(requireContext())
            }
        }

        makeNotificationFixedSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isLoading) {
                AppPreferences.nextLessonNotificationsFixed = checked

                //If we have any notification, we have to update them:
                startNextLessonNotificationService()
            }
        }

        isLoading = false
    }

    private fun updateCalendarFontPreview(fontSize: Int) = fontSizePreview.setTextSize(COMPLEX_UNIT_SP, fontSize.toFloat())

    ///////////////////////////
    ////NEXT LESSON NOTIFICATION
    ///////////////////////////

    private fun startNextLessonNotificationService() {
        activity!!.startService(NextLessonNotificationService.createIntent(
                activity!!, NLNStarter.NOTIFICATIONS_SWITCHED_ON)
        )
    }

    ////////////////////////////
    // SEARCH FOR LESSON CHANGES
    ////////////////////////////
    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //Does not uses menus, nothing to bind!
            Unit


    inner class ChangeStudyCourseDialog(context: Context) : AlertDialog(context) {

        /**
         * Dispatched when the user changed or did not change the study course and just pressed OK.
         */
        val onChoiceMade = Signal1<StudyCourse>()

        init {
            setView(Views.inflate(context, R.layout.dialog_change_study_course))

            courseSelector.selectStudyCourse(AppPreferences.studyCourse)

            changeButton.visibility = View.GONE
            changeButton.setOnClickListener {
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

            courseSelector.onCoursesLoaded.connect {
                changeButton.visibility = View.VISIBLE
            }
            courseSelector.loadCourses()

        }

        private fun dealWithNextLessonNotifications() {
            NextLessonNotificationService.clearNotifications(context)

            //We need to show the next lesson notification for the new course
            activity!!.startService(NextLessonNotificationService.createIntent(
                    context, NLNStarter.STUDY_COURSE_CHANGE
            ))
        }

        private fun removeOverlappingExtraCourses(course: StudyCourse) {
            val overlappingExtraCourses = AppPreferences.getExtraCoursesOfCourse(course)
            overlappingExtraCourses.forEach {
                ColorDispenser.dissociateColorFromType(it.lessonTypeId)
                AppPreferences.removeExtraCourse(it.lessonTypeId)
            }
        }

        private fun clearFilters() {
            AppPreferences.removeAllHiddenCourses() //No longer need them
        }

        private fun clearCache(selectedCourse: StudyCourse) {
            //We changed our course, let's wipe out all the cache!
            Networker.purgeStudyCourseCache()

            //If we've just selected a course that we already had in our extra course, we need to
            //delete that course from cache
            for (overlappingExtras in AppPreferences.getExtraCoursesOfCourse(selectedCourse)) {
                Networker.purgeExtraCourse(overlappingExtras.lessonTypeId)
            }
        }

    }

    companion object {
        const val MIN_CALENDAR_FONT_SIZE = 7
    }

}
