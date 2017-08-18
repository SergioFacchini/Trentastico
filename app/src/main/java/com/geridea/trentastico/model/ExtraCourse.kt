package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger

import org.json.JSONException
import org.json.JSONObject

class ExtraCourse {
    var lessonTypeId: Int = 0
        private set
    var courseId: Long = 0
        private set
    var year: Int = 0
        private set

    var name: String? = null
        private set
    var studyCourseFullName: String? = null
        private set

    var color: Int = 0
        private set

    private constructor() {}

    constructor(lessonTypeId: Int, courseId: Long, year: Int, name: String?, studyCourse: String, color: Int) {
        this.lessonTypeId = lessonTypeId
        this.courseId = courseId
        this.year = year
        this.name = name
        this.studyCourseFullName = studyCourse
        this.color = color
    }

    constructor(studyCourse: StudyCourse, lessonType: LessonType) : this(
            lessonType.id,
            studyCourse.courseId,
            studyCourse.year,
            lessonType.name,
            studyCourse.generateFullDescription(),
            lessonType.color
    ) {
    }

    fun toJSON(): JSONObject {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("lessonTypeId", lessonTypeId)
            jsonObject.put("courseId", courseId)
            jsonObject.put("year", year)
            jsonObject.put("name", name)
            jsonObject.put("studyCourse", studyCourseFullName)
            jsonObject.put("color", color)

            jsonObject
        } catch (e: JSONException) {
            BugLogger.logBug("Converting extra course to JSON", e)
            throw RuntimeException("Could not convert extra course to JSON")
        }

    }

    val courseAndYear: CourseAndYear
        get() {
            val cay = CourseAndYear()
            cay.courseId = courseId
            cay.year = year
            return cay
        }

    /**
     * @return true if the scheduled lesson refers to the same subject of this extra course.
     */
    fun isLessonOfCourse(lesson: LessonSchedule): Boolean = lesson.lessonTypeId == lessonTypeId.toLong()

    companion object {

        fun fromJSON(json: JSONObject): ExtraCourse {
            return try {
                val course = ExtraCourse()
                course.lessonTypeId = json.getInt("lessonTypeId")
                course.courseId = json.getLong("courseId")
                course.year = json.getInt("year")

                course.name = json.getString("name")
                course.studyCourseFullName = json.getString("studyCourse")
                course.color = json.getInt("color")

                course
            } catch (e: JSONException) {
                BugLogger.logBug("Parsing extra course", e)
                throw RuntimeException("Could not convert JSON to extra course")
            }

        }
    }
}
