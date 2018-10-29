package com.geridea.trentastico.gui.fragments

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.AdapterView
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.adapters.LessonTypesAdapter
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener
import com.geridea.trentastico.services.NextLessonNotificationShowService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ColorDispenser
import com.geridea.trentastico.utils.UIUtils
import com.threerings.signals.Signal0
import kotlinx.android.synthetic.main.dialog_extra_course_search.*
import kotlinx.android.synthetic.main.dialog_extra_course_search.view.*

internal class ExtraCourseSearchDialog(
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
        view.selectedCourseTextView.text = "Sto cercando le lezioni del corso da te selezionato: " +
                studyCourse.generateFullDescription()

        view.lessonsFound.visibility = View.GONE

        setView(view)
    }

    override fun onAttachedToWindow() {
        cancel_search.setOnClickListener { dismiss() }

        lessonsListCourseSearch.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            val lessonType = lessonsListCourseSearch.getItemAtPosition(position) as LessonType

            if (canLessonTypeBeSelected(lessonType)) {
                ColorDispenser.associateColorToTypeIfNeeded(lessonType.id)
                AppPreferences.addExtraCourse(ExtraCourse(
                        lessonType.id, lessonType.name, lessonType.teachers, lessonType.partitioningName,
                        lessonType.kindOfLesson, studyCourse
                ))

                NextLessonNotificationShowService.clearNotifications(context, false)
                NextLessonNotificationShowService.scheduleNowIfEnabled()

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

            lessonsListCourseSearch.adapter = LessonTypesAdapter(context, lessonTypes, lessonTypesOfCourse!!)
        }

        searchingLessons.visibility = View.GONE
        lessonsFound.visibility = View.VISIBLE
    }
}