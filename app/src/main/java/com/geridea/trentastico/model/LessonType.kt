package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 20/08/2017.
 */

/**
 * @param kindOfLesson "Lezione", "Laboratorio", ecc...
 */
data class LessonType(
        val id: String,
        val name: String,
        val teachers: List<String>,
        var partitioningName: String?,
        val kindOfLesson: String,
        var isVisible: Boolean = true) {

    /**
     * @param teachersNamesUnparsed a "," separated list of teachers' names
     */
    constructor(
            id: String,
            name: String,
            teachersNamesUnparsed: String,
            partitioningName: String?,
            kindOfLesson: String,
            isVisible: Boolean
    ): this(id, name, createTeachers(teachersNamesUnparsed), partitioningName,
            kindOfLesson, isVisible)

    constructor(extraCourse: ExtraCourse, isVisible: Boolean): this(
            extraCourse.lessonTypeId,
            extraCourse.lessonName,
            extraCourse.teachers,
            extraCourse.partitioningName,
            extraCourse.kindOfLesson,
            isVisible
    )

    override fun equals(other: Any?): Boolean =
            //There were some problems with original equals due to the fact that lesson types might
            //be set visible or not. This caused one visible and one hidden lesson type to be
            //considered different
            if (other is LessonType) other.id == this.id else false

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + buildTeachersNamesOrDefault().hashCode()
        return result
    }

    companion object {

        private fun createTeachers(teachersNamesUnparsed: String): List<String> {
            return if (teachersNamesUnparsed.isBlank()) {
                arrayListOf()
            } else {
                teachersNamesUnparsed.split(',')
            }
        }

    }

    /**
     * Creates a list of teaches, separated by a comma. In case there isn't any teacher, then the
     * default "no teacher" placeholder is returned.
     */
    fun buildTeachersNamesOrDefault(): String {
        return if (teachers.isEmpty()) NO_TEACHER_ASSIGNED_DEFAULT_TEXT
        else teachers.joinToString { it }
    }

}

val NO_TEACHER_ASSIGNED_DEFAULT_TEXT: String = "(nessun insegnante assegnato)"