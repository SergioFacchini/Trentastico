package com.geridea.trentastico.services


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LessonSchedule
import com.geridea.trentastico.utils.ArrayUtils
import com.geridea.trentastico.utils.JsonUtils

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.HashSet

/**
 * Keeps track of which notification were already shown and decides what notification which
 * notifications should be shown again.
 */
class ShownNotificationsTracker {

    private val shownNotificationsIds: HashSet<Long>
    private val roomlessLessonsIds: HashSet<Long>

    constructor(shownIds: HashSet<Long>, roomlessIds: HashSet<Long>) {
        shownNotificationsIds = shownIds
        roomlessLessonsIds = roomlessIds
    }

    constructor() {
        shownNotificationsIds = HashSet()
        roomlessLessonsIds = HashSet()
    }

    /**
     * Tells whenever a next lesson notification has to be shown for the specified lesson found
     * because of a specified starter. A lesson notification has to be shown when:
     *
     *  * It's about a lesson that the user has not been notified about
     *  * We changed a setting about notifications, so all the notifications have to be
     * displayed all over again.
     *  * The notification didn't have a room
     *
     * A lesson notification won't be shown when:
     *
     *  * We've already shown that notification (and the user might have dismissed it)
     *
     * @param lesson the lesson to inquire about
     * @param starter the starter of the service
     * @return true if the notification should be shown, false otherwise
     */
    fun shouldNotificationBeShown(lesson: LessonSchedule, starter: NLNStarter): Boolean {
        return if (shownNotificationsIds.contains(lesson.id)) {
            //We already have shown the notification to the user. Let's check if we should
            //reshow that
            if (didLessonGainRoom(lesson)) {
                //We could not fetch the room of the given lesson and now we have internet. The
                //lesson is now probably updated.
                true
            } else {
                ArrayUtils.isOneOf(starter,
                        NLNStarter.PHONE_BOOT,
                        NLNStarter.STUDY_COURSE_CHANGE,
                        NLNStarter.NOTIFICATIONS_SWITCHED_ON)
            }
        } else {
            true
        }

    }

    private fun didLessonGainRoom(lesson: LessonSchedule): Boolean {
        return lesson.hasRoomSpecified() && isLessonWithoutRoom(lesson)
    }

    private fun isLessonWithoutRoom(lesson: LessonSchedule): Boolean {
        return roomlessLessonsIds.contains(lesson.id)
    }

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
    fun clear() {
        shownNotificationsIds.clear()
    }

    fun toJson(): JSONObject {
        try {
            val json = JSONObject()

            json.put("shown-notifications", JsonUtils.collectionToArray(shownNotificationsIds))
            json.put("roomless-notifications", JsonUtils.collectionToArray(roomlessLessonsIds))

            return json
        } catch (e: JSONException) {
            e.printStackTrace()
            BugLogger.logBug("Cannot convert notification tracker to json", e)

            throw RuntimeException(e)
        }

    }

    /**
     * Notifies this tracked that the lessons passed as parameter don't have rooms
     * @param lessonsWithoutRooms the lessons without rooms
     */
    fun notifyLessonsWithoutRoom(lessonsWithoutRooms: ArrayList<LessonSchedule>) {
        for (lessonsWithoutRoom in lessonsWithoutRooms) {
            roomlessLessonsIds.add(lessonsWithoutRoom.id)
        }
    }

    companion object {

        fun fromJson(jsonString: String): ShownNotificationsTracker {

            return if (jsonString == "{}") {
                ShownNotificationsTracker()
            } else {
                try {
                    val jsonObject = JSONObject(jsonString)

                    val shownIds = JsonUtils.getLongHashSet(
                            jsonObject.getJSONArray("shown-notifications")
                    )

                    val roomlessIds = JsonUtils.getLongHashSet(
                            jsonObject.getJSONArray("roomless-notifications")
                    )

                    ShownNotificationsTracker(shownIds, roomlessIds)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    BugLogger.logBug("Cannot convert notification json to notifications tracker", e)

                    throw RuntimeException(e)
                }

            }
        }
    }

}
