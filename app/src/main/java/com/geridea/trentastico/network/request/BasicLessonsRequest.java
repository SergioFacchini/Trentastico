package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.support.annotation.NonNull;

import com.geridea.trentastico.Config;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.utils.time.CalendarInterval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

abstract class BasicLessonsRequest implements IRequest {

    private static int PROGRESSIVE_OPERATION_ID_COUNTER = 1;

    private final int operationId;

    public BasicLessonsRequest() {
        this.operationId = PROGRESSIVE_OPERATION_ID_COUNTER++;
    }

    @Override
    public String getURL() {
        return buildRequestURL(getCourseId(), getYear(), getCalendarIntervalToLoad());
    }

    protected abstract CalendarInterval getCalendarIntervalToLoad();
    protected abstract long getCourseId();
    protected abstract int getYear();

    protected static String buildRequestURL(long courseId, int year, CalendarInterval intervalToLoad) {
        if(Config.DEBUG_MODE && Config.LAUNCH_REQUESTS_TO_DEBUG_SERVER){
            return Config.DEBUG_SERVER_URL;
        }

        return String.format(
                Locale.CANADA,
                "http://webapps.unitn.it/Orari/it/Web/AjaxEventi/c/%d-%d/agendaWeek?_=%d&start=%d&end=%d",
                courseId,
                year,
                System.currentTimeMillis(),
                intervalToLoad.getFrom().getTimeInMillis() / 1000,
                intervalToLoad.getTo()  .getTimeInMillis() / 1000
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
