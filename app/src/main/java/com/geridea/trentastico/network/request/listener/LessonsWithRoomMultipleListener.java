package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;

import java.util.ArrayList;

public class LessonsWithRoomMultipleListener implements LessonWithRoomFetchedListener {

    private int numOfRequestReceived;
    private final ArrayList<LessonSchedule> lessonsThatWillBeUpdated;
    private final LessonsWithRoomListener listener;

    public LessonsWithRoomMultipleListener(ArrayList<LessonSchedule> lessons, LessonsWithRoomListener listener) {
        this.lessonsThatWillBeUpdated = lessons;
        this.listener = listener;
    }

    @Override
    public void onLessonUpdated(LessonSchedule updatedLessons) {
        numOfRequestReceived++;
        if (numOfRequestReceived == lessonsThatWillBeUpdated.size()) {
            listener.onLoadingCompleted(lessonsThatWillBeUpdated);
        }
    }

}
