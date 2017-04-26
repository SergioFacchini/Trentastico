package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;

public interface LessonWithRoomFetchedListener {
    void onUpdateSuccessful(LessonSchedule lesson);
    void onError(LessonSchedule lesson);
}
