package trentastico.geridea.com.trentastico.activities.gui.network;

import com.android.volley.VolleyError;

import java.util.Calendar;

import trentastico.geridea.com.trentastico.activities.model.LessonsSet;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public interface LessonsFetchedListener {
    void onLessonsLoaded(LessonsSet lessons, Calendar from, Calendar to);

    void onErrorHappened(VolleyError error);

    void onParsingErrorHappened(Exception e);
}
