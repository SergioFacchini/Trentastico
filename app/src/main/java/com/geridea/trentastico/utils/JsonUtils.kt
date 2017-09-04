package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import org.json.JSONArray
import org.json.JSONException
import java.util.*

object JsonUtils {
    /**
     * @return a new [JSONArray] containing the elements of the collection.
     */
    fun <T> collectionToArray(collection: Collection<T>): JSONArray {
        val shownNotificationsIdsArray = JSONArray()
        for (shownNotificationsId in collection) {
            shownNotificationsIdsArray.put(shownNotificationsId)
        }
        return shownNotificationsIdsArray
    }

    @Throws(JSONException::class)
    fun getLongHashSet(jsonArray: JSONArray): HashSet<Long> =
            (0 until jsonArray.length()).mapTo(HashSet()) { jsonArray.getLong(it) }
}
