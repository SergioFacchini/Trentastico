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

    constructor(json: JSONObject) : this(
            courseId   = json.getString("courseId"),
            courseName = json.getString("courseName"),
            yearId     = json.getString("yearId"),
            yearName   = json.getString("yearName")
    )

    fun generateFullDescription(): String = "$courseName > $yearName"

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("courseId", courseId)
        json.put("courseName", courseName)
        json.put("yearId", yearId)
        json.put("yearName", yearName)

        return json
    }

}
