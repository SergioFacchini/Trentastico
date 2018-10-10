package com.geridea.trentastico.gui.fragments

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.View
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.utils.AppPreferences
import com.threerings.signals.Signal0
import kotlinx.android.synthetic.main.dialog_extra_course_add.*
import kotlinx.android.synthetic.main.dialog_extra_course_add.view.*

internal class ExtraCourseAddDialog(context: Context) : AlertDialog(context) {

    /**
     * Dispatched when the user has selected and added a new study course.
     */
    val onNewCourseAdded = Signal0()

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_extra_course_add)

        view.cannotSelectCurrentStudyCourse.visibility = View.GONE
        view.searchForLessonsButton.visibility = View.GONE

        view.courseSelector.loadCourses()
        view.courseSelector.onCourseChanged.connect { newStudyCourse ->
            if (newStudyCourse == AppPreferences.studyCourse) {
                view.cannotSelectCurrentStudyCourse.visibility = View.VISIBLE
                view.searchForLessonsButton.visibility = View.GONE
            } else {
                view.cannotSelectCurrentStudyCourse.visibility = View.GONE
                view.searchForLessonsButton.visibility = View.VISIBLE
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

            val dialog = ExtraCourseSearchDialog(context, selectedStudyCourse)
            dialog.onCourseSelectedAndAdded.connect {
                onNewCourseAdded.dispatch()
                dismiss()
            }
            dialog.show()
            dialog.searchForCourses()
        }
    }


}