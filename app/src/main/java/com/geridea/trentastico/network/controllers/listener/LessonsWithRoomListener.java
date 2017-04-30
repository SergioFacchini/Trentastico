package com.geridea.trentastico.network.controllers.listener;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;

import java.util.ArrayList;

public interface LessonsWithRoomListener {
    /**
     * @param updatedLessons all lessons (with and without rooms)
     * @param lessonsWithoutRooms lessons for which we could not fetch the roms
     */
    void onLoadingCompleted(ArrayList<LessonSchedule> updatedLessons, ArrayList<LessonSchedule> lessonsWithoutRooms);
}
