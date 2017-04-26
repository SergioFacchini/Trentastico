package com.geridea.trentastico.services;


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.utils.JsonUtils;
import com.geridea.trentastico.utils.NumbersUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Keeps track of which notification were already shown and decides what notification which
 * notifications should be shown again.
 */
public class ShownNotificationsTracker {

    private final HashSet<Long> shownNotificationsIds;
    private final HashSet<Long> roomlessLessonsIds;

    public ShownNotificationsTracker(HashSet<Long> shownIds, HashSet<Long> roomlessIds) {
        shownNotificationsIds = shownIds;
        roomlessLessonsIds    = roomlessIds;
    }

    public ShownNotificationsTracker() {
        shownNotificationsIds = new HashSet<>();
        roomlessLessonsIds    = new HashSet<>();
    }

    /**
     * Tells whenever a next lesson notification has to be shown for the specified lesson found
     * because of a specified starter. A lesson notification has to be shown when:
     * <ul>
     *     <li>It's about a lesson that the user has not been notified about</li>
     *     <li>We changed a setting about notifications, so all the notifications have to be
     *         displayed all over again.</li>
     *     <li>The notification didn't have a room</li>
     * </ul>
     * A lesson notification won't be shown when:
     * <ul>
     *     <li>We've already shown that notification (and the user might have dismissed it)</li>
     * </ul>
     * @param lesson the lesson to inquire about
     * @param starter the starter of the service
     * @return true if the notification should be shown, false otherwise
     */
    public boolean shouldNotificationBeShown(LessonSchedule lesson, int starter) {
        if (shownNotificationsIds.contains(lesson.getId())) {
            //We already have shown the notification to the user. Let's check if we should
            //reshow that
            if (didLessonGainRoom(lesson)) {
                //We could not fetch the room of the given lesson and now we have internet. The
                //lesson is now probably updated.
                return true;
            } else {
                return NumbersUtils.isOneOf(starter,
                    NextLessonNotificationService.STARTER_PHONE_BOOT,
                    NextLessonNotificationService.STARTER_STUDY_COURSE_CHANGE,
                    NextLessonNotificationService.STARTER_NOTIFICATIONS_SWITCHED_ON);
            }
        } else {
            return true;
        }

    }

    private boolean didLessonGainRoom(LessonSchedule lesson) {
        return lesson.hasRoomSpecified() && isLessonWithoutRoom(lesson);
    }

    private boolean isLessonWithoutRoom(LessonSchedule lesson) {
        return roomlessLessonsIds.contains(lesson.getId());
    }

    /**
     * Makes the tracker take note that a notification has been shown to a specific lesson
     * @param lessonId the id of the lesson fr which the notification has been shown
     */
    public void notifyNotificationShown(long lessonId) {
        shownNotificationsIds.add(lessonId);
    }

    /**
     * Removes the tracking of all the already shown notifications. Must be called when the
     * currently shown notifications are no longer valid.
     */
    public void clear() {
        shownNotificationsIds.clear();
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();

            json.put("shown-notifications",    JsonUtils.collectionToArray(shownNotificationsIds));
            json.put("roomless-notifications", JsonUtils.collectionToArray(roomlessLessonsIds));

            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            BugLogger.logBug("Cannot convert notification tracker to json", e);

            throw new RuntimeException(e);
        }
    }

    public static ShownNotificationsTracker fromJson(String jsonString) {

        if (jsonString.equals("{}")) {
            return new ShownNotificationsTracker();
        } else {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);

                HashSet<Long> shownIds = JsonUtils.getLongHashSet(
                    jsonObject.getJSONArray("shown-notifications")
                );

                HashSet<Long> roomlessIds = JsonUtils.getLongHashSet(
                    jsonObject.getJSONArray("roomless-notifications")
                );

                return new ShownNotificationsTracker(shownIds, roomlessIds);
            } catch (JSONException e) {
                e.printStackTrace();
                BugLogger.logBug("Cannot convert notification json to notifications tracker", e);

                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Notifies this tracked that the lessons passed as parameter don't have rooms
     * @param lessonsWithoutRooms the lessons without rooms
     */
    public void notifyLessonsWithoutRoom(ArrayList<LessonSchedule> lessonsWithoutRooms) {
        for (LessonSchedule lessonsWithoutRoom : lessonsWithoutRooms) {
            roomlessLessonsIds.add(lessonsWithoutRoom.getId());
        }
    }

}
