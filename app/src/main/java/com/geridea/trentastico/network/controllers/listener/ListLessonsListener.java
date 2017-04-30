package com.geridea.trentastico.network.controllers.listener;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.model.LessonType;

import java.util.Collection;

public interface ListLessonsListener {
    void onErrorHappened(Exception error);

    void onParsingErrorHappened(Exception e);

    void onLessonTypesRetrieved(Collection<LessonType> lessonTypes);
}
