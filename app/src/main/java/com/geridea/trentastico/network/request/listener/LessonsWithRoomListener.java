package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;

import java.util.ArrayList;

public interface LessonsWithRoomListener {
    void onLoadingCompleted(ArrayList<LessonSchedule> updatedLessons);
}
