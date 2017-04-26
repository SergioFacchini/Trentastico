package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashSet;

public class JsonUtils {
    /**
     * @return a new {@link JSONArray} containing the elements of the collection.
     */
    @NonNull
    public static <T> JSONArray collectionToArray(Collection<T> collection) {
        JSONArray shownNotificationsIdsArray = new JSONArray();
        for (T shownNotificationsId : collection) {
            shownNotificationsIdsArray.put(shownNotificationsId);
        }
        return shownNotificationsIdsArray;
    }

    @NonNull
    public static HashSet<Long> getLongHashSet(JSONArray jsonArray) throws JSONException {
        HashSet<Long> longs = new HashSet<>();
        for(int i = 0; i< jsonArray.length(); i++){
            longs.add(jsonArray.getLong(i));
        }
        return longs;
    }
}
