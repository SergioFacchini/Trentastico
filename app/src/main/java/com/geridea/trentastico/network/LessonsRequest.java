package com.geridea.trentastico.network;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.threerings.signals.Listener0;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public class LessonsRequest extends StringRequest implements Response.Listener<String> {

    private static final DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
        15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    );

    private final Calendar fromWhen;
    private final Calendar toWhen;
    private final StudyCourse studyCourse;
    private final LessonsFetchedListener listener;

    /**
     * Dispatched when the request has been successfully fulfilled.
     */
    public final Signal1<LessonsSet> onRequestSuccessful = new Signal1<>();

    /**
     * Dispatched right before the request is about to be sent.
     */
    public final Signal0 inRequestAboutToBeSent = new Signal0();

    /**
     * Dispatched when the request has encountered an error while trying to parse the response.
     */
    public final Signal1<Exception> onParsingErrorHappened = new Signal1<>();

    /**
     * Dispatched when there is an error while trying to get lessons from internet.
     */
    public final Signal1<VolleyError> onNetworkErrorHappened = new Signal1<>();


    public LessonsRequest(Calendar fromWhen, Calendar toWhen, StudyCourse studyCourse, final LessonsFetchedListener listener) {
        super(Method.GET, buildRequestURL(studyCourse, fromWhen, toWhen), null, null);

        this.fromWhen = fromWhen;
        this.toWhen = toWhen;
        this.studyCourse = studyCourse;
        this.listener = listener;

        setRetryPolicy(retryPolicy);

        inRequestAboutToBeSent.connect(new Listener0() {
            @Override
            public void apply() {
                listener.onLoadingAboutToStart(LessonsRequest.this.fromWhen, LessonsRequest.this.toWhen);
            }
        });
    }

    public StudyCourse getStudyCourse() {
        return studyCourse;
    }

    public Calendar getFromWhen() {
        return fromWhen;
    }

    public long getFromWhenMs() {
        return getFromWhen().getTimeInMillis();
    }

    public Calendar getToWhen() {
        return toWhen;
    }

    public long getToWhenMs() {
        return getToWhen().getTimeInMillis();
    }


    //We cannot provide the listeners to the constructor, so we use this workaround do still manage
    //the listeners.
    protected void deliverResponse(String response) {
        onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        listener.onErrorHappened(error);
        onNetworkErrorHappened.dispatch(error);
    }

    private static String buildRequestURL(StudyCourse studyCourse, Calendar from, Calendar to) {
        return String.format(
                Locale.CANADA,
                "http://webapps.unitn.it/Orari/it/Web/AjaxEventi/c/%d-%d/agendaWeek?start=%d&end=%d",
                studyCourse.getCourseId(),
                studyCourse.getYear(),
                from.getTimeInMillis() / 1000,
                to.getTimeInMillis() / 1000
        );
    }

    @Override
    public void onResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);

            LessonsSet lessonsSet = new LessonsSet();
            JSONArray activitiesJson = jsonResponse.getJSONArray("Attivita");
            for(int i = 0; i<activitiesJson.length(); i++){
                lessonsSet.addLessonType(LessonType.fromJson(activitiesJson.getJSONObject(i)));
            }

            JSONArray eventsJson = jsonResponse.getJSONArray("Eventi");
            for(int i = 0; i<eventsJson.length(); i++){
                lessonsSet.addLessonSchedule(LessonSchedule.fromJson(eventsJson.getJSONObject(i)));
            }

            listener.onLessonsLoaded(lessonsSet, fromWhen, toWhen);
            onRequestSuccessful.dispatch(lessonsSet);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onParsingErrorHappened(e);

            onParsingErrorHappened.dispatch(e);
        }
    }
}
