package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import butterknife.*
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.gui.adapters.ExtraCoursesAdapter
import com.geridea.trentastico.gui.adapters.LessonTypesAdapter
import com.geridea.trentastico.gui.views.CourseSelectorView
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.threerings.signals.Signal0

class ExtraLessonsFragment : FragmentWithMenuItems() {

    @BindView(R.id.extra_lessons_list) lateinit var lessonsList: ListView
    @BindView(R.id.no_extra_courses_label) lateinit var noExtraCoursesLabel: TextView

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_extra_lessons, container, false)
        ButterKnife.bind(this, view)

        initLessonsList()

        return view
    }

    private fun initLessonsList() {
        val extraCourses = AppPreferences.extraCourses
        if (extraCourses.isEmpty()) {
            noExtraCoursesLabel.visibility = View.VISIBLE
        } else {
            noExtraCoursesLabel.visibility = View.GONE
        }
        lessonsList.adapter = ExtraCoursesAdapter(activity, extraCourses)
    }

    @OnItemLongClick(R.id.extra_lessons_list)
    internal fun onItemLongClick(position: Int): Boolean {
        val course = lessonsList.getItemAtPosition(position) as ExtraCourse

        val dialog = ExtraCourseDeleteDialog(activity, course)
        dialog.onDeleteConfirm.connect {
            initLessonsList()

            //Updating notifications
            NextLessonNotificationService.removeNotificationsOfExtraCourse(context, course)
            NextLessonNotificationService.createIntent(
                    activity, NLNStarter.EXTRA_COURSE_CHANGE
            )
        }
        dialog.show()

        return true
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = intArrayOf(R.id.menu_add_extra_lessons)

    override fun bindMenuItem(item: MenuItem) {
        if (item.itemId == R.id.menu_add_extra_lessons) {
            item.setOnMenuItemClickListener {
                val dialog = ExtraCourseAddDialog(activity)
                dialog.onNewCourseAdded.connect {
                    initLessonsList()

                    //Updating notifications
                    NextLessonNotificationService.createIntent(
                            activity, NLNStarter.EXTRA_COURSE_CHANGE
                    )
                }
                dialog.show()
                true
            }
        }
    }

    internal inner class ExtraCourseDeleteDialog(context: Context, private val course: ExtraCourse): AlertDialog(context) {

        @BindView(R.id.course_name)       lateinit var courseName: TextView
        @BindView(R.id.teacher_name)      lateinit var teacherName: TextView
        @BindView(R.id.partitioning_name) lateinit var partitioningName: TextView
        @BindView(R.id.study_course_name) lateinit var studyCourseName: TextView

        @BindView(R.id.color) lateinit var color: ImageView

        /**
         * Dispatched when the user has selected and added a new study course.
         */
        val onDeleteConfirm = Signal0()

        init {
            val view = Views.inflate<View>(context, R.layout.dialog_extra_course_delete)
            ButterKnife.bind(this, view)

            courseName     .text = course.lessonName
            studyCourseName.text = course.fullName
            teacherName    .text = course.teachersNames

            if (course.partitioningName != null) {
                partitioningName.text = course.partitioningName
            } else {
                partitioningName.visibility = View.GONE
            }

            color.setImageDrawable(ColorDrawable(course.color))

            setView(view)
        }


        @OnClick(R.id.cancel_button)
        fun onCancelButtonPressed() = dismiss()

        @OnClick(R.id.delete_button)
        fun onDeleteButtonPressed() {
            AppPreferences.removeExtraCourse(course.lessonTypeId)
            Networker.purgeExtraCourse(course.lessonTypeId)

            onDeleteConfirm.dispatch()
            dismiss()
        }

    }

    internal inner class ExtraCourseAddDialog(context: Context) : AlertDialog(context) {

        @BindView(R.id.cannot_select_current_study_course)
        lateinit var cannotSelectCurrentStudyCourse: TextView

        @BindView(R.id.course_selector)
        lateinit var courseSelector: CourseSelectorView

        @BindView(R.id.search_for_lessons)
        lateinit var searchForLessonsButton: Button

        /**
         * Dispatched when the user has selected and added a new study course.
         */
        val onNewCourseAdded = Signal0()

        init {

            val view = Views.inflate<View>(context, R.layout.dialog_extra_course_add)
            ButterKnife.bind(this, view)

            cannotSelectCurrentStudyCourse.visibility = GONE

            courseSelector.loadCourses()
            courseSelector.onCourseChanged.connect { newStudyCourse ->
                if (newStudyCourse == AppPreferences.studyCourse) {
                    cannotSelectCurrentStudyCourse.visibility = View.VISIBLE
                    searchForLessonsButton.visibility = View.GONE
                } else {
                    cannotSelectCurrentStudyCourse.visibility = GONE
                    searchForLessonsButton.visibility = View.VISIBLE
                }
            }

            setView(view)
        }

        @OnClick(R.id.search_for_lessons)
        fun onSearchForLessonsButtonClicked() {
            val selectedStudyCourse = courseSelector.buildStudyCourse()
            if (selectedStudyCourse == AppPreferences.studyCourse) {
                cannotSelectCurrentStudyCourse.visibility = View.VISIBLE
                searchForLessonsButton.isEnabled = false
                return
            }

            val dialog = ExtraCourseSearchDialog(activity, selectedStudyCourse)
            dialog.onCourseSelectedAndAdded.connect {
                onNewCourseAdded.dispatch()
                dismiss()
            }
            dialog.show()
            dialog.searchForCourses()
        }

    }

    internal inner class ExtraCourseSearchDialog(
            context: Context,
            private val studyCourse: StudyCourse) : AlertDialog(context), ListLessonsListener {

        @BindView(R.id.searching_lessons)
        lateinit var searchingLessons: View
        @BindView(R.id.lessons_found)
        lateinit var lessonsFound: View

        @BindView(R.id.selected_course)
        lateinit var selectedCourseTextView: TextView

        @BindView(R.id.error_while_searching)
        lateinit var errorWhileSearching: TextView

        @BindView(R.id.lessons_found_text)
        lateinit var lessonsFoundText: TextView

        @BindView(R.id.lessons_list)
        lateinit var lessonsList: ListView

        /**
         * Dispatched when the user has selected a course and that course has been added to preferences.
         */
        val onCourseSelectedAndAdded = Signal0()

        init {

            val view = Views.inflate<View>(context, R.layout.dialog_extra_course_search)
            ButterKnife.bind(this, view)

            selectedCourseTextView.text = "Sto cercando le lezioni del corso da te selezionato: " +
                    studyCourse.generateFullDescription()

            lessonsFound.visibility = GONE

            setView(view)
        }

        fun searchForCourses() = Networker.loadLessonTypesOfStudyCourse(studyCourse, this)

        @OnClick(R.id.cancel_search)
        fun onCancelSearchButtonPressed() = dismiss()

        @OnItemClick(R.id.lessons_list)
        fun onLessonSelected(position: Int) {
            val lesson = lessonsList.getItemAtPosition(position) as LessonTypeNew

            if (!AppPreferences.hasExtraCourseWithId(lesson.id)) {
                AppPreferences.addExtraCourse(
                    ExtraCourse(
                            lesson.id, lesson.name, lesson.teachersNames,
                            lesson.partitioningName, studyCourse, lesson.color)
                )

                dismiss()
                onCourseSelectedAndAdded.dispatch()
            }
        }

        override fun onErrorHappened(error: Exception) = showErrorMessage()

        private fun showErrorMessage() = activity.runOnUiThread {
            errorWhileSearching.visibility = View.VISIBLE
        }

        override fun onParsingErrorHappened(e: Exception) = showErrorMessage()

        override fun onLessonTypesRetrieved(lessonTypes: Collection<LessonTypeNew>) = activity.runOnUiThread {
            if (lessonTypes.isEmpty()) {
                lessonsFoundText.text = "Non vi sono lezioni nel corso di studi selezionato; probabilmente le lezioni non sono ancora state pianificate dall'università. Se pensi che sia un errore, torna indietro e controlla di aver selezionato il corso corretto."
            } else {
                lessonsFoundText.text = "Ho trovato le seguenti lezioni. Premi sulla lezione che ti interessa seguire per aggiungerla al calendario."

                lessonsList.adapter = LessonTypesAdapter(context, lessonTypes)
            }

            searchingLessons.visibility = View.GONE
            lessonsFound.visibility     = View.VISIBLE
        }
    }
}
