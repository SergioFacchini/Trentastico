package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;

import java.util.ArrayList;

public class LessonsWithRoomMultipleListener implements LessonWithRoomFetchedListener {

    private int numOfRequestReceived;
    private final ArrayList<LessonSchedule> lessonsThatWillBeUpdated;
    private final ArrayList<LessonSchedule> lessonsWithoutRooms;
    private final ArrayList<LessonSchedule> lessonsWithRooms;
    private final LessonsWithRoomListener listener;

    /**
     *
     * @param lessonsToLoad the lessons that need the room to be loaded
     * @param lessonsWithRooms lessons that already have rooms. The will just need to be dispatched
     *                         as they are, merged with the lessons that gained rooms.
     * @param listener the listener
     */
    public LessonsWithRoomMultipleListener(ArrayList<LessonSchedule> lessonsToLoad, ArrayList<LessonSchedule> lessonsWithRooms, LessonsWithRoomListener listener) {
        this.lessonsThatWillBeUpdated = lessonsToLoad;
        this.lessonsWithRooms         = lessonsWithRooms;
        this.listener                 = listener;

        this.lessonsWithoutRooms = new ArrayList<>();
    }

    @Override
    public void onUpdateSuccessful(LessonSchedule updatedLessons) {
        updateCounterAndCheckIfCompleted();
    }

    private void updateCounterAndCheckIfCompleted() {
        numOfRequestReceived++;
        if (numOfRequestReceived == lessonsThatWillBeUpdated.size()) {
            lessonsThatWillBeUpdated.addAll(lessonsWithRooms);
            listener.onLoadingCompleted(lessonsThatWillBeUpdated, lessonsWithoutRooms);
        }
    }

    @Override
    public void onError(LessonSchedule lesson) {
        lessonsWithoutRooms.add(lesson);

        updateCounterAndCheckIfCompleted();
    }

}
