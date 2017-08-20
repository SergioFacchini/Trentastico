package com.geridea.trentastico.network.request


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.time.CalendarUtils
import com.geridea.trentastico.utils.time.CalendarUtils.debuggableMillis
import java.io.Serializable
import java.util.*


class LessonsDiffResult : Serializable {

    val removedLessons = ArrayList<LessonSchedule>()
    val addedLessons = ArrayList<LessonSchedule>()

    val changedLessons = ArrayList<LessonChange>()

    fun addRemovedLesson(lesson: LessonSchedule) {
        removedLessons.add(lesson)
    }

    fun addAddedLesson(lesson: LessonSchedule) {
        addedLessons.add(lesson)
    }

    fun addChangedLesson(original: LessonSchedule, changed: LessonSchedule) {
        changedLessons.add(LessonChange(original, changed))
    }

    fun addFrom(anotherDiff: LessonsDiffResult) {
        addedLessons.addAll(anotherDiff.addedLessons)
        removedLessons.addAll(anotherDiff.removedLessons)
        changedLessons.addAll(anotherDiff.changedLessons)
    }

    val isEmpty: Boolean
        get() = addedLessons.isEmpty() && removedLessons.isEmpty() && changedLessons.isEmpty()

    val numTotalDifferences: Int
        get() = addedLessons.size + removedLessons.size + changedLessons.size

    fun discardPastLessons() {
        discardPastLessons(addedLessons)
        discardPastLessons(removedLessons)
        discardPastLessonsChanges(changedLessons)
    }

    private fun discardPastLessonsChanges(lessons: ArrayList<LessonChange>) {
        val currentMillis = debuggableMillis

        val iterator = lessons.iterator()
        while (iterator.hasNext()) {
            val lesson = iterator.next()
            if (lesson.original.startsBefore(currentMillis)) {
                iterator.remove()
            }
        }
    }

    private fun discardPastLessons(lessons: ArrayList<LessonSchedule>) {
        val currentMillis = debuggableMillis

        val iterator = lessons.iterator()
        while (iterator.hasNext()) {
            val lesson = iterator.next()
            if (lesson.startsBefore(currentMillis)) {
                iterator.remove()
            }
        }
    }

    inner class LessonChange(val original: LessonSchedule, val changed: LessonSchedule) : Serializable {

        val differences: Collection<*>
            get() {
                val differences = ArrayList<String>(3)
                var descriptionDetailsChanged = false

                if (original.startsAt != changed.startsAt) {
                    val day = CalendarUtils.formatEEEEDDMMMM(changed.startsAt)
                    val hours = CalendarUtils.formatHHMM(changed.startsAt)
                    differences.add(String.format("Lezione spostata a %s alle ore %s", day, hours))
                }

                if (original.durationInMinutes != changed.durationInMinutes) {
                    differences.add(
                            String.format("La lezione durerà %d min invece di %d.",
                                    changed.durationInMinutes,
                                    original.durationInMinutes
                            )
                    )
                }

                if (original.room != changed.room) {
                    differences.add(
                            "La lezione si terrà in \"" + changed.room + "\" invece di \"" + original.room + "\""
                    )
                    descriptionDetailsChanged = true
                }

                if (original.subject != changed.subject) {
                    differences.add(
                            String.format("Il corso della lezione è stato modificato in \"%s\".", changed.subject)
                    )
                    descriptionDetailsChanged = true
                }

                if (!descriptionDetailsChanged && original.eventDescription != changed.eventDescription) {
                    differences.add(
                            "I dettagli della lezione sono cambiati:-------------\n " + changed.eventDescription
                    )
                }

                return differences
            }
    }
}
