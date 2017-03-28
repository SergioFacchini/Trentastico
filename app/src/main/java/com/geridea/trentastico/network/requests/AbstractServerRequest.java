package com.geridea.trentastico.network.requests;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.EnqueueableOperation;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public abstract class AbstractServerRequest extends StringRequest implements EnqueueableOperation {

    private static final DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
        15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    );

    private static int PROGRESSIVE_OPERATION_ID_COUNTER = 1;

    private int operationId;

    AbstractServerRequest() {
        super(Method.GET, null, null, null);

        operationId = PROGRESSIVE_OPERATION_ID_COUNTER++;

        setRetryPolicy(retryPolicy);
    }

    @Override
    public String getUrl() {
        return buildRequestURL(getStudyCourse(), getIntervalToLoad());
    }

    //We cannot provide the listeners to the constructor, so we use this workaround do still manage
    //the listeners.
    protected void deliverResponse(String response) {
        onResponse(response);
    }

    protected abstract void onResponse(String response);

    @Override
    abstract public void deliverError(VolleyError error);

    abstract public StudyCourse getStudyCourse();

    abstract public WeekInterval getIntervalToLoad();

    private static String buildRequestURL(StudyCourse studyCourse, WeekInterval intervalToLoad) {
        CalendarInterval interval = intervalToLoad.toCalendarInterval();

        return String.format(
                Locale.CANADA,
                "http://webapps.unitn.it/Orari/it/Web/AjaxEventi/c/%d-%d/agendaWeek?_=%d&start=%d&end=%d",
                studyCourse.getCourseId(),
                studyCourse.getYear(),
                System.currentTimeMillis(),
                interval.getFrom().getTimeInMillis() / 1000,
                interval.getTo()  .getTimeInMillis() / 1000
        );
    }

    @NonNull
    protected LessonsSet parseResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        LessonsSet lessonsSet = new LessonsSet();
        JSONArray activitiesJson = jsonResponse.getJSONArray("Attivita");
        parseLessonTypes(lessonsSet, activitiesJson);

        JSONArray lessonsJson = jsonResponse.getJSONArray("Eventi");
        lessonsSet.addLessonSchedules(createLessonSchedulesFromJSON(lessonsJson));
        return lessonsSet;
    }

    private void parseLessonTypes(LessonsSet lessonsSet, JSONArray activitiesJson) throws JSONException {
        for(int i = 0; i<activitiesJson.length(); i++){
            lessonsSet.addLessonType(LessonType.fromJson(activitiesJson.getJSONObject(i)));
        }
    }

    @NonNull
    private ArrayList<LessonSchedule> createLessonSchedulesFromJSON(JSONArray eventsJson) throws JSONException {
        ArrayList<LessonSchedule> schedules = new ArrayList<>();
        for(int i = 0; i<eventsJson.length(); i++){
            schedules.add(LessonSchedule.fromJson(eventsJson.getJSONObject(i)));
        }
        return schedules;
    }

    public int getOperationId() {
        return operationId;
    }
}
