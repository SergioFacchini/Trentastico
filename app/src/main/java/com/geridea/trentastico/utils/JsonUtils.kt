package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

object JsonUtils {
    /**
     * @return a new [JSONArray] containing the elements of the collection.
     */
    fun <T> collectionToArray(collection: Collection<T>): JSONArray {
        val jsonArray = JSONArray()
        for (item in collection) {
            jsonArray.put(item)
        }
        return jsonArray
    }

    @Throws(JSONException::class)
    fun getLongHashSet(jsonArray: JSONArray): HashSet<Long> =
            (0 until jsonArray.length()).mapTo(HashSet()) { jsonArray.getLong(it) }
}

fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> =
        (0 until length()).map { mapper(getJSONObject(it)) }

fun JSONArray.toStringArray(): List<String> =
        (0 until length()).map { getString(it) }

fun <T> List<T>.toJsonStringArray(converter: (T) -> JSONObject): JSONArray {
    val jsonArray = JSONArray()
    forEach { jsonArray.put(converter(it)) }
    return jsonArray
}

fun List<String>.toJsonStringArray(): JSONArray {
    val jsonArray = JSONArray()
    forEach { jsonArray.put(it) }
    return jsonArray
}