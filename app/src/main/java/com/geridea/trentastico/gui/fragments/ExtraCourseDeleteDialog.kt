package com.geridea.trentastico.gui.fragments

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AlertDialog
import android.view.View
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ColorDispenser
import com.threerings.signals.Signal0
import kotlinx.android.synthetic.main.dialog_extra_course_delete.*
import kotlinx.android.synthetic.main.itm_extra_course.view.*


internal class ExtraCourseDeleteDialog(context: Context, private val course: ExtraCourse) : AlertDialog(context) {

    /**
     * Dispatched when the user has selected and added a new study course.
     */
    val onDeleteConfirm = Signal0()

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_extra_course_delete)

        view.courseName     .text = course.lessonName
        view.studyCourseName.text = course.fullName
        view.teacherName    .text = course.buildTeachersNamesOrDefault()

        if (course.partitioningName != null) {
            view.partitioningName.text = course.partitioningName
        } else {
            view.partitioningName.visibility = View.GONE
        }

        view.color.setImageDrawable(ColorDrawable(ColorDispenser.getColor(course.lessonTypeId)))

        setView(view)
    }

    override fun onAttachedToWindow() {
        cancel_button.setOnClickListener { dismiss() }

        delete_button.setOnClickListener {
            AppPreferences.removeExtraCourse(course.lessonTypeId)
            Networker.purgeExtraCourse(course.lessonTypeId)
            ColorDispenser.dissociateColorFromType(course.lessonTypeId)


            NextLessonNotificationService.clearNotifications(context)
            NextLessonNotificationService.scheduleNowIfEnabled()

            onDeleteConfirm.dispatch()
            dismiss()
        }
    }

}