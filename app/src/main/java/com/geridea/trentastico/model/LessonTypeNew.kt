package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 20/08/2017.
 */

/**
 * @param kindOfLesson "Lezione", "Laboratorio", ecc...
 */
data class LessonTypeNew(
        val id: String,
        val name: String,
        /**
         * The list of teachers, separated by a comma
         */
        val teachers: List<Teacher>,
        val partitioningName: String?,
        val color: Int,
        val kindOfLesson: String,
        var isVisible: Boolean = true) {

    constructor(
            id: String,
            name: String,
            teachersNamesUnparsed: String,
            /**
             * A "," separated list of teachers' codes
             */
            teachersCodesToParse: String,
            partitioningName: String?,
            color: Int,
            kindOfLesson: String,
            isVisible: Boolean
    ): this(id, name, createTeachers(teachersNamesUnparsed, teachersCodesToParse), partitioningName,
            color, kindOfLesson, isVisible)

    constructor(extraCourse: ExtraCourse, isVisible: Boolean): this(
            extraCourse.lessonTypeId,
            extraCourse.lessonName,
            extraCourse.teachers,
            extraCourse.partitioningName,
            extraCourse.color,
            extraCourse.kindOfLesson,
            isVisible
    )

    override fun equals(other: Any?): Boolean =
            //There were some problems with original equals due to the fact that lesson types might
            //be set visible or not. This caused one visible and one hidden lesson type to be
            //considered different
            if (other is LessonTypeNew) other.id == this.id else false

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + buildTeachersNamesOrDefault().hashCode()
        return result
    }

    companion object {

        private fun createTeachers(teachersNamesUnparsed: String, teachersCodesToParse: String): List<Teacher> {
            return if (teachersNamesUnparsed.isBlank()) {
                arrayListOf()
            } else {
                val teacherNames = teachersNamesUnparsed.split(',').map { it.trim() }
                val teacherCodes = teachersCodesToParse .split(',').map { it.trim() }

                teacherCodes.mapIndexed { index, id -> Teacher(id, teacherNames[index])}
            }
        }

    }

    /**
     * Creates a list of teaches, separated by a comma. In case there isn't any teacher, then the
     * default "no teacher" placeholder is returned.
     */
    fun buildTeachersNamesOrDefault(): String {
        return if (teachers.isEmpty()) NO_TEACHER_ASSIGNED_DEFAULT_TEXT
        else teachers.joinToString { it.name }
    }

}
