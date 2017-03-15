package com.geridea.trentastico.network;

import com.android.volley.VolleyError;

import java.util.Calendar;

import com.geridea.trentastico.model.LessonsSet;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public interface LessonsFetchedListener {
    void onLoadingAboutToStart(Calendar from, Calendar to);

    void onLessonsLoaded(LessonsSet lessons, Calendar from, Calendar to);

    void onErrorHappened(VolleyError error);

    void onParsingErrorHappened(Exception e);
}
