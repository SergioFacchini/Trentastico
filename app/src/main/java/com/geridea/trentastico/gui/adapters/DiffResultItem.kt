package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 01/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.network.request.LessonsDiffResult
import com.geridea.trentastico.utils.StringUtils
import com.geridea.trentastico.utils.time.CalendarUtils

abstract class DiffResultItem(private val lesson: LessonSchedule) {

    val courseName: String
        get() = lesson.subject

    val scheduledDay: String
        get() = CalendarUtils.formatEEEEDDMMMM(lesson.startsAt)

    val scheduledHours: String
        get() = CalendarUtils.formatHHMM(lesson.startsAt)

    val duration: Int
        get() = lesson.durationInMinutes

    val color: Int
        get() = lesson.color

    abstract val diffDescription: String

    abstract val modifications: String?

    private class AddDiffResult(lesson: LessonSchedule) : DiffResultItem(lesson) {

        override val diffDescription: String
            get() = "È stata aggiunta questa lezione:"

        override val modifications: String?
            get() = null
    }

    private class RemoveDiffResult(lesson: LessonSchedule) : DiffResultItem(lesson) {

        override val diffDescription: String
            get() = "Questa lezione è stata annullata:"

        override val modifications: String?
            get() = null
    }

    private class ChangeDiffResult(private val lessonChange: LessonsDiffResult.LessonChange) : DiffResultItem(lessonChange.original) {

        override val diffDescription: String
            get() = "La lezione ha subito variazioni:"

        override val modifications: String
            get() = StringUtils.implode(lessonChange.differences, "\n")
    }

    companion object {

        fun buildAdded(lesson: LessonSchedule): DiffResultItem = AddDiffResult(lesson)

        fun buildRemoved(lesson: LessonSchedule): DiffResultItem = RemoveDiffResult(lesson)

        fun buildChanged(change: LessonsDiffResult.LessonChange): DiffResultItem = ChangeDiffResult(change)
    }


}
