package trentastico.geridea.com.trentastico.activities.gui.network;

import com.android.volley.VolleyError;

import trentastico.geridea.com.trentastico.activities.model.LessonsSet;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public interface LessonsFetchedListener {
    void onLessonsLoaded(LessonsSet lessons);

    void onErrorHappened(VolleyError error);

    void onParsingErrorHappened(Exception e);
}
