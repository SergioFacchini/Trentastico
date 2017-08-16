package com.geridea.trentastico.model

import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.utils.AppPreferences
import java.util.*

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
open class LessonsSet {

    val scheduledLessons: HashMap<Long, LessonSchedule> = HashMap()
    val lessonTypes: HashMap<Int, LessonType> = HashMap()

    fun addLessonType(lessonType: LessonType) {
        lessonTypes.put(lessonType.id, lessonType)
    }

    fun mergeWith(lessons: LessonsSet) {
        //When the filtering dialog is opened, the new, updated lessons set will not update the ones
        //that are already shown in the dialog. By doing the following we're ensuring that the
        //lesson types visibilities are consistent.
        for (lessonTypeToAdd in lessons.lessonTypes.values) {
            val existingLessonType = lessonTypes[lessonTypeToAdd.id]

            if (existingLessonType != null) {
                existingLessonType.isVisible = lessonTypeToAdd.isVisible
                existingLessonType.mergePartitionings(lessonTypeToAdd.partitioning)
            } else {
                lessonTypes.put(lessonTypeToAdd.id, lessonTypeToAdd)
            }
        }

        for (lessonSchedule in lessons.scheduledLessons.values) {
            scheduledLessons.put(lessonSchedule.id, lessonSchedule)
        }
    }

    fun recalculatePartitionings() {
        for (lessonType in lessonTypes.values) {

            var current = Partitioning.NONE
            for (lesson in LessonSchedule.getLessonsOfType(lessonType, this.scheduledLessons.values)) {

                val found = LessonType.findPartitioningFromDescription(lesson.fullDescription)
                if (found.type != PartitioningType.NONE) {
                    if (current.type == PartitioningType.NONE) {
                        //We found the first partitioning
                        current = found
                    } else if (found.type == current.type) {
                        current.mergePartitionCases(found)
                    } else {
                        //We found two lessons with different partitioning methods. We just ignore this
                        //situation for now and consider it to be not partitioned.
                        BugLogger.logBug(
                                "Two lessons with different partitioning methods",
                                RuntimeException("Two lessons with different partitioning methods")
                        )
                        current = Partitioning.NONE
                        break
                    }
                }
            }

            current.hidePartitioningsInList(AppPreferences.getHiddenPartitionings(lessonType.id.toLong()))

            //Some lessons have strings like "Mod.2" to te that it's the second part of the course.
            //These are not partitionings. This kind of situation can be found by checking the
            //number of cases (if there is only one case then it's probably not a partitioning
            //string).
            //Fixes #3
            if (current.hasMoreThanOneCase()) {
                lessonType.partitioning = current
            } else {
                lessonType.partitioning = Partitioning.NONE
            }
        }

    }

    fun addLessonSchedules(lessons: ArrayList<LessonSchedule>) {
        for (lesson in lessons) {
            scheduledLessons.put(lesson.id, lesson)
        }

        recalculatePartitionings()
    }

    fun removeLessonTypesNotInCurrentSemester() {
        lessonTypes.values.removeAll(lessonTypes.values.filter {
            val lessons = LessonSchedule.getLessonsOfType(it, this.scheduledLessons.values)
            lessons.none { Semester.isInCurrentSemester(it) }
        })
    }

    /**
     * Removes all lessons and lesson types not matching the lesson type passed in the
     * parameter.
     */
    fun prepareForExtraCourse(extraCourse: ExtraCourse) {
        lessonTypes.values.removeAll(
            lessonTypes.values.filter { it.id != extraCourse.lessonTypeId }
        )

        scheduledLessons.values.removeAll(
            scheduledLessons.values.filter { it.lessonTypeId != extraCourse.lessonTypeId.toLong() }
        )
    }

    fun filterLessons() {
        LessonSchedule.filterLessons(this.scheduledLessons.values)
    }

}
