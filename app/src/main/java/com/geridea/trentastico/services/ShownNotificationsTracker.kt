package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.JsonUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Keeps track of which notification were already shown and decides what notification which
 * notifications should be shown again.
 */
class ShownNotificationsTracker {

    private val shownNotificationsIds: HashSet<Int>

    constructor(shownIds: HashSet<Int>) {
        shownNotificationsIds = shownIds
    }

    constructor() {
        shownNotificationsIds = HashSet()
    }

    fun shouldNotificationBeShown(lesson: LessonSchedule): Boolean =
        !shownNotificationsIds.contains(lesson.id.hashCode())

    /**
     * Makes the tracker take note that a notification has been shown to a specific lesson
     * @param lesson the lesson for which the notification has been shown
     */
    fun notifyNotificationShown(lesson: LessonSchedule) {
        shownNotificationsIds.add(lesson.id.hashCode())
        save()
    }

    private fun save() {
        AppPreferences.notificationTracker = this
    }

    /**
     * Removes the tracking of all the already shown notifications. Must be called when the
     * currently shown notifications are no longer valid.
     */
    fun clear() {
        shownNotificationsIds.clear()
        save()
    }

    fun toJson(): JSONObject {
        return try {
            val json = JSONObject()

            json.put("shown-notifications", JsonUtils.collectionToArray(shownNotificationsIds))

            json
        } catch (e: JSONException) {
            e.printStackTrace()
            BugLogger.logBug("Cannot convert notification tracker to json", e)

            throw RuntimeException(e)
        }

    }

    fun getShownNotificationIds() = shownNotificationsIds.iterator().asSequence().toList()

    companion object {

        fun fromJson(jsonString: String): ShownNotificationsTracker =
                if (jsonString == "{}") {
                    ShownNotificationsTracker()
                } else {
                    try {
                        val jsonObject = JSONObject(jsonString)

                        val shownIds = JsonUtils.getIntHashSet(
                                jsonObject.getJSONArray("shown-notifications")
                        )

                        ShownNotificationsTracker(shownIds)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        BugLogger.logBug("Cannot convert notification json to notifications tracker", e)

                        throw RuntimeException(e)
                    }

                }
    }

}
