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
import android.widget.AdapterView
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.gui.adapters.ExtraCoursesAdapter
import com.geridea.trentastico.gui.adapters.LessonTypesAdapter
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ColorDispenser
import com.geridea.trentastico.utils.UIUtils
import com.threerings.signals.Signal0
import kotlinx.android.synthetic.main.dialog_extra_course_add.*
import kotlinx.android.synthetic.main.dialog_extra_course_delete.*
import kotlinx.android.synthetic.main.dialog_extra_course_search.*
import kotlinx.android.synthetic.main.fragment_extra_lessons.*
import kotlinx.android.synthetic.main.itm_extra_course.*

class ExtraLessonsFragment : FragmentWithMenuItems() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_extra_lessons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLessonsList()

        lessonsList.setOnItemLongClickListener { _, _, position, _ ->
            val course = lessonsList.getItemAtPosition(position) as ExtraCourse

            val dialog = ExtraCourseDeleteDialog(requireContext(), course)
            dialog.onDeleteConfirm.connect {
                initLessonsList()

                //Updating notifications
                NextLessonNotificationService.removeNotificationsOfExtraCourse(requireContext(), course)
                NextLessonNotificationService.createIntent(requireContext(), NLNStarter.EXTRA_COURSE_CHANGE)
            }
            dialog.show()

            return@setOnItemLongClickListener true
        }

    }

    private fun initLessonsList() {
        val extraCourses = AppPreferences.extraCourses
        if (extraCourses.isEmpty()) {
            noExtraCoursesLabel.visibility = View.VISIBLE
        } else {
            noExtraCoursesLabel.visibility = View.GONE
        }
        lessonsList.adapter = ExtraCoursesAdapter(requireContext(), extraCourses)
    }


    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = intArrayOf(R.id.menu_add_extra_lessons)

    override fun bindMenuItem(item: MenuItem) {
        if (item.itemId == R.id.menu_add_extra_lessons) {
            item.setOnMenuItemClickListener {
                val dialog = ExtraCourseAddDialog(requireContext())
                dialog.onNewCourseAdded.connect {
                    initLessonsList()

                    //Updating notifications
                    NextLessonNotificationService.createIntent(
                            requireContext(), NLNStarter.EXTRA_COURSE_CHANGE
                    )
                }
                dialog.show()
                true
            }
        }
    }

    internal inner class ExtraCourseDeleteDialog(context: Context, private val course: ExtraCourse): AlertDialog(context) {

        /**
         * Dispatched when the user has selected and added a new study course.
         */
        val onDeleteConfirm = Signal0()

        init {
            val view = Views.inflate<View>(context, R.layout.dialog_extra_course_delete)

            courseName     .text = course.lessonName
            studyCourseName.text = course.fullName
            teacherName    .text = course.buildTeachersNamesOrDefault()

            if (course.partitioningName != null) {
                partitioningName.text = course.partitioningName
            } else {
                partitioningName.visibility = View.GONE
            }

            color.setImageDrawable(ColorDrawable(ColorDispenser.getColor(course.lessonTypeId)))

            setView(view)
        }

        override fun onAttachedToWindow() {
            cancel_button.setOnClickListener { dismiss() }

            delete_button.setOnClickListener {
                AppPreferences.removeExtraCourse(course.lessonTypeId)
                Networker     .purgeExtraCourse (course.lessonTypeId)
                ColorDispenser.dissociateColorFromType(course.lessonTypeId)

                onDeleteConfirm.dispatch()
                dismiss()
            }
        }

    }

    internal inner class ExtraCourseAddDialog(context: Context) : AlertDialog(context) {

        /**
         * Dispatched when the user has selected and added a new study course.
         */
        val onNewCourseAdded = Signal0()

        init {

            val view = Views.inflate<View>(context, R.layout.dialog_extra_course_add)

            cannotSelectCurrentStudyCourse.visibility = GONE
            searchForLessonsButton.visibility = View.GONE

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

        override fun onAttachedToWindow() {
            searchForLessonsButton.setOnClickListener {
                val selectedStudyCourse = courseSelector.buildStudyCourse()
                if (selectedStudyCourse == AppPreferences.studyCourse) {
                    cannotSelectCurrentStudyCourse.visibility = View.VISIBLE
                    searchForLessonsButton.isEnabled = false
                    return@setOnClickListener
                }

                val dialog = ExtraCourseSearchDialog(requireContext(), selectedStudyCourse)
                dialog.onCourseSelectedAndAdded.connect {
                    onNewCourseAdded.dispatch()
                    dismiss()
                }
                dialog.show()
                dialog.searchForCourses()
            }
        }


    }

    internal inner class ExtraCourseSearchDialog(
            context: Context,
            private val studyCourse: StudyCourse) : AlertDialog(context), ListLessonsListener {

        /**
         * List of lessons of the actually selected standard course. The user should be not able to
         * add a lesson that he/she is already taking
         */
        private var lessonTypesOfCourse: List<LessonType>? = null

        /**
         * Dispatched when the user has selected a course and that course has been added to preferences.
         */
        val onCourseSelectedAndAdded = Signal0()

        init {

            val view = Views.inflate<View>(context, R.layout.dialog_extra_course_search)
            selectedCourseTextView.text = "Sto cercando le lezioni del corso da te selezionato: " +
                    studyCourse.generateFullDescription()

            lessonsFound.visibility = GONE

            setView(view)
        }

        override fun onAttachedToWindow() {
            cancel_search.setOnClickListener { dismiss() }

            lessonsList.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
                val lessonType = lessonsListCourseSearch.getItemAtPosition(position) as LessonType

                if (canLessonTypeBeSelected(lessonType)) {
                    ColorDispenser.associateColorToTypeIfNeeded(lessonType.id)
                    AppPreferences.addExtraCourse(ExtraCourse(
                            lessonType.id, lessonType.name, lessonType.teachers, lessonType.partitioningName,
                            lessonType.kindOfLesson, studyCourse
                    ))

                    dismiss()
                    onCourseSelectedAndAdded.dispatch()
                }
            }

        }

        fun searchForCourses() {
            Networker.loadCachedLessonTypes { lessonTypes ->
                lessonTypesOfCourse = lessonTypes

                Networker.loadLessonTypesOfStudyCourse(studyCourse, this@ExtraCourseSearchDialog)
            }
        }

        private fun canLessonTypeBeSelected(lesson: LessonType) =
                !AppPreferences.hasExtraCourseWithId(lesson.id) &&
                        lessonTypesOfCourse!!.none { it.id == lesson.id }

        override fun onErrorHappened(error: Exception) = showErrorMessage()

        private fun showErrorMessage() = UIUtils.runOnMainThread {
            errorWhileSearching.visibility = View.VISIBLE
        }

        override fun onParsingErrorHappened(e: Exception) = showErrorMessage()

        override fun onLessonTypesRetrieved(lessonTypes: Collection<LessonType>) = UIUtils.runOnMainThread {
            if (lessonTypes.isEmpty()) {
                lessonsFoundText.text = "Non vi sono lezioni nel corso di studi selezionato; probabilmente le lezioni non sono ancora state pianificate dall'università. Se pensi che sia un errore, torna indietro e controlla di aver selezionato il corso corretto."
            } else {
                lessonsFoundText.text = "Ho trovato le seguenti lezioni. Premi sulla lezione che ti interessa seguire per aggiungerla al calendario."

                lessonsList.adapter = LessonTypesAdapter(context, lessonTypes, lessonTypesOfCourse!!)
            }

            searchingLessons.visibility = View.GONE
            lessonsFound.visibility     = View.VISIBLE
        }
    }
}
