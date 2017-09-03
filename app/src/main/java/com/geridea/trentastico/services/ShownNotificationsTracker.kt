package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.JsonUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Keeps track of which notification were already shown and decides what notification which
 * notifications should be shown again.
 */
class ShownNotificationsTracker {

    private val shownNotificationsIds: HashSet<Long>

    constructor(shownIds: HashSet<Long>) {
        shownNotificationsIds = shownIds
    }

    constructor() {
        shownNotificationsIds = HashSet()
    }

    /**
     * Tells whenever a next lesson notification has to be shown for the specified lesson found
     * because of a specified starter. A lesson notification has to be shown when:
     *
     *  * It's about a lesson that the user has not been notified about
     *  * We changed a setting about notifications, so all the notifications have to be
     * displayed all over again.
     *
     * A lesson notification won't be shown when:
     *
     *  * We've already shown that notification (and the user might have dismissed it)
     *
     * @param lesson the lesson to inquire about
     * @param starter the starter of the service
     * @return true if the notification should be shown, false otherwise
     */
    fun shouldNotificationBeShown(lesson: LessonSchedule, starter: NLNStarter): Boolean =
            true //TODO: rework notifications
//        if (!shownNotificationsIds.contains(lesson.id)) true
//        else ArrayUtils.isOneOf(starter,
//                                NLNStarter.PHONE_BOOT,
//                                NLNStarter.STUDY_COURSE_CHANGE,
//                                NLNStarter.NOTIFICATIONS_SWITCHED_ON)

    /**
     * Makes the tracker take note that a notification has been shown to a specific lesson
     * @param lessonId the id of the lesson fr which the notification has been shown
     */
    fun notifyNotificationShown(lessonId: Long) {
        shownNotificationsIds.add(lessonId)
    }

    /**
     * Removes the tracking of all the already shown notifications. Must be called when the
     * currently shown notifications are no longer valid.
     */
    fun clear() = shownNotificationsIds.clear()

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

    companion object {

        fun fromJson(jsonString: String): ShownNotificationsTracker = if (jsonString == "{}") {
            ShownNotificationsTracker()
        } else {
            try {
                val jsonObject = JSONObject(jsonString)

                val shownIds = JsonUtils.getLongHashSet(
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