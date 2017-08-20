package com.geridea.trentastico.model

import java.util.*

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
open class LessonsSet {

    val scheduledLessons: HashMap<Long, LessonSchedule> = HashMap()

    val lessonTypes: HashMap<String, LessonType> = HashMap()

    fun addLessonType(lessonType: LessonType) {
        lessonTypes.put(lessonType.id, lessonType)
    }

    fun recalculatePartitionings() {
        //TODO: implement partitionings
//        for (lessonType in lessonTypes.values) {
//
//            var current = Partitioning.NONE
//            for (lesson in LessonSchedule.getLessonsOfType(lessonType, this.scheduledLessons.values)) {
//
//                val found = LessonType.findPartitioningFromDescription(lesson.fullDescription)
//                if (found.type != PartitioningType.NONE) {
//                    if (current.type == PartitioningType.NONE) {
//                        //We found the first partitioning
//                        current = found
//                    } else if (found.type == current.type) {
//                        current.mergePartitionCases(found)
//                    } else {
//                        //We found two lessons with different partitioning methods. We just ignore this
//                        //situation for now and consider it to be not partitioned.
//                        BugLogger.logBug(
//                                "Two lessons with different partitioning methods",
//                                RuntimeException("Two lessons with different partitioning methods")
//                        )
//                        current = Partitioning.NONE
//                        break
//                    }
//                }
//            }
//
//            current.hidePartitioningsInList(AppPreferences.getHiddenPartitionings(lessonType.id.toLong()))
//
//            //Some lessons have strings like "Mod.2" to te that it's the second part of the course.
//            //These are not partitionings. This kind of situation can be found by checking the
//            //number of cases (if there is only one case then it's probably not a partitioning
//            //string).
//            //Fixes #3
//            if (current.hasMoreThanOneCase()) {
//                lessonType.partitioning = current
//            } else {
//                lessonType.partitioning = Partitioning.NONE
//            }
//        }
    }

    fun addLessonSchedules(lessons: ArrayList<LessonSchedule>) {
//        for (lesson in lessons) {
//            scheduledLessons.put(lesson.id, lesson)
//        }

        recalculatePartitionings()
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
            scheduledLessons.values.filter { it.lessonTypeId != extraCourse.lessonTypeId }
        )
    }

    fun filterLessons() = LessonSchedule.filterLessons(this.scheduledLessons.values)

}
