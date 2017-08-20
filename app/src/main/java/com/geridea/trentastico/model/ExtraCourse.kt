package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger
import org.json.JSONException
import org.json.JSONObject

class ExtraCourse(
        val lessonTypeId: String,
        val lessonName: String,
        val partitioningName: String,
        val studyCourse: StudyCourse,
        val color: Int) {

    /**
     * The complete name of the extra course (name of the course + lesson name)
     */
    val fullName
      get() = "$studyCourse > $lessonName"

    fun toJSON(): JSONObject {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("lessonTypeId", lessonTypeId)
            jsonObject.put("lessonName", lessonName)
            jsonObject.put("partitioningName", partitioningName)
            jsonObject.put("studyCourse", studyCourse.toJson())
            jsonObject.put("color", color)

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
                    partitioningName = json.getString("partitioningName"),
                    studyCourse      = StudyCourse.fromStringJson(json.getString("studyCourse")),
                    color            = json.getInt("color")
                )
            } catch (e: JSONException) {
                BugLogger.logBug("Parsing extra course", e)
                throw RuntimeException("Could not convert JSON to extra course")
            }

        }
    }

    fun isPartOfCourse(studyCourse: StudyCourse): Boolean = (studyCourse == this.studyCourse)

}
