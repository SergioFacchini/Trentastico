package com.geridea.trentastico.model

import org.json.JSONObject


/*
 * Created with ♥ by Slava on 05/09/2017.
 */
data class Teacher(val id: String, val name: String) {

    val teacherPhotoUrl: String
        get () {
            //Some IDs are shorter that they should be. I fix them there:
            val fixedId = id.padStart(7, '0')
            return "http://webapps.unitn.it/People/Foto/PER$fixedId.jpg"
        }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        return json
    }

    constructor(json: JSONObject) : this(json.getString("id"), json.getString("name"))

    companion object {
        val PLACEHOLDER_TEACHER: Teacher = Teacher("00000000", NO_TEACHER_ASSIGNED_DEFAULT_TEXT)
        val PLACEHOLDER_TEACHER_LIST = listOf(PLACEHOLDER_TEACHER)
    }

}

val NO_TEACHER_ASSIGNED_DEFAULT_TEXT: String = "(nessun insegnante assegnato)"
