package com.geridea.trentastico.network.request


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.time.CalendarUtils
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

    val isEmpty: Boolean
        get() = addedLessons.isEmpty() && removedLessons.isEmpty() && changedLessons.isEmpty()

    val hasSomeDifferences: Boolean
        get() = !isEmpty

    val numTotalDifferences: Int
        get() = addedLessons.size + removedLessons.size + changedLessons.size

    fun discardPassedChanges() {
        val currentMillis = System.currentTimeMillis()

        addedLessons.removeAll {   it.startsBefore(currentMillis)}
        removedLessons.removeAll { it.startsBefore(currentMillis)}
        changedLessons.removeAll { it.changed.startsBefore(currentMillis)}
    }

    /**
     * Removes all the changes that occurs after the passed timestamp
     */
    fun discardChangesAfterTimestamp(timestamp: Long) {
        discardChangesAfterTimestamp(timestamp, addedLessons)
        discardChangesAfterTimestamp(timestamp, removedLessons)
        discardPastLessonsChanges(timestamp, changedLessons)
    }

    private fun discardPastLessonsChanges(timestamp: Long, lessons: ArrayList<LessonChange>) =
        lessons.removeAll { it.original.startsAfter(timestamp) && it.changed.startsAfter(timestamp) }


    private fun discardChangesAfterTimestamp(timestamp: Long, lessons: ArrayList<LessonSchedule>) =
        lessons.removeAll { it.startsAfter(timestamp) }


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

                if (original.roomComplete != changed.roomComplete) {
                    differences.add(
                            "La lezione si terrà in \"" + changed.roomComplete + "\" invece di \"" + original.roomComplete + "\""
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
