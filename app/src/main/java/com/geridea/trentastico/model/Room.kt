package com.geridea.trentastico.model

import org.json.JSONObject


/*
 * Created with ♥ by Slava on 06/09/2017.
 */

data class Room(val room: String, val department: String?) {

    constructor(json: JSONObject) : this(json.getString("room"), json.optString("department", null))

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("room", room)
        json.putOpt("department", department)

        return json
    }

}