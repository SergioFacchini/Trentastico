package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.utils.mapObjects
import com.geridea.trentastico.utils.toJsonArray
import org.json.JSONException
import org.json.JSONObject

class ExtraCourse(
        val lessonTypeId: String,
        val lessonName: String,
        val teachers: List<Teacher>,
        val partitioningName: String?,
        val kindOfLesson: String,
        val studyCourse: StudyCourse) {

    /**
     * The complete name of the extra course (name of the course + lesson name)
     */
    val fullName
      get() = "${studyCourse.courseName} > ${studyCourse.yearName}"

    fun toJSON(): JSONObject {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("lessonTypeId",     lessonTypeId)
            jsonObject.put("lessonName",       lessonName)
            jsonObject.put("teachers",         teachers.toJsonArray { it.toJson() })
            jsonObject.put("partitioningName", partitioningName)
            jsonObject.put("kindOfLesson",     kindOfLesson)
            jsonObject.put("studyCourse",      studyCourse.toJson())
            return jsonObject
        } catch (e: JSONException) {
            BugLogger.logBug("Converting extra course to JSON", e)
            throw RuntimeException("Could not convert extra course to JSON")
        }

    }

    /**
     * @return true if the scheduled lesson refers to the same subject of this extra course.
     */
    fun isLessonOfCourse(lesson: LessonSchedule): Boolean = lesson.lessonTypeId == lessonTypeId

    companion object {

        fun fromJSON(json: JSONObject): ExtraCourse {
            try {
                 return ExtraCourse(
                    lessonTypeId     = json.getString("lessonTypeId"),
                    lessonName       = json.getString("lessonName"),
                    teachers         = json.getJSONArray("teachers").mapObjects { Teacher(it) },
                    partitioningName = json.optString("partitioningName", null),
                    kindOfLesson     = json.getString("kindOfLesson"),
                    studyCourse      = StudyCourse(json.getJSONObject("studyCourse"))
                )
            } catch (e: JSONException) {
                BugLogger.logBug("Parsing extra course", e)
                throw RuntimeException("Could not convert JSON to extra course")
            }

        }
    }

    fun isPartOfCourse(studyCourse: StudyCourse): Boolean = (studyCourse == this.studyCourse)

    /**
     * Creates a list of teaches, separated by a comma. In case there isn't any teacher, then the
     * default "no teacher" placeholder is returned.
     */
    fun buildTeachersNamesOrDefault(): String {
        return if (teachers.isEmpty()) NO_TEACHER_ASSIGNED_DEFAULT_TEXT
        else teachers.joinToString { it.name }
    }

}
