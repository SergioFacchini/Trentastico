package com.geridea.trentastico.model

import org.json.JSONObject

/**
 * Keeps track of the id of the course and a year of that course. Useful to identify a specific
 * study course.
 */
data class StudyCourse(
        var courseId: String,
        var courseName: String,
        var yearId: String,
        var yearName: String) {

    fun generateFullDescription(): String = "$courseName > $yearName"

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("courseId", courseId)
        json.put("courseName", courseName)
        json.put("yearId", yearId)
        json.put("yearName", yearName)

        return json
    }

    companion object {

        fun fromJson(string: JSONObject): StudyCourse = StudyCourse(
            courseId   = string.getString("courseId"),
            courseName = string.getString("courseName"),
            yearId     = string.getString("yearId"),
            yearName   = string.getString("yearName")
        )

        fun fromStringJson(string: String): StudyCourse = fromJson(JSONObject(string))

    }

}
