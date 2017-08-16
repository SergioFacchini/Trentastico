package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.HashSet

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
    fun getLongHashSet(jsonArray: JSONArray): HashSet<Long> {
        val longs = HashSet<Long>()
        for (i in 0..jsonArray.length() - 1) {
            longs.add(jsonArray.getLong(i))
        }
        return longs
    }
}
