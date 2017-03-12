package trentastico.geridea.com.trentastico.activities.gui.network;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.threerings.signals.Signal0;
import com.threerings.signals.Signal1;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

import trentastico.geridea.com.trentastico.activities.model.LessonSchedule;
import trentastico.geridea.com.trentastico.activities.model.LessonType;
import trentastico.geridea.com.trentastico.activities.model.LessonsSet;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public class LessonsRequest extends StringRequest implements Response.Listener<String>, Response.ErrorListener {

    private static final DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
        15000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    );

    private final Calendar fromWhen;
    private final Calendar toWhen;
    private final StudyCourse studyCourse;
    private final LessonsFetchedListener listener;

    /**
     * Dispatched when the request has been fulfilled (either fetched some data or a network error
     * happened).
     */
    public final Signal0 onRequestTerminated = new Signal0();

    /**
     * Dispatched when the request has been successfully fulfilled.
     */
    public final Signal1<LessonsSet> onRequestSuccessful = new Signal1<>();

    /**
     * Dispatched right before the request is about to be sent.
     */
    public final Signal0 inRequestAboutToBeSent = new Signal0();


    public LessonsRequest(Calendar fromWhen, Calendar toWhen, StudyCourse studyCourse, final LessonsFetchedListener listener) {
        super(Method.GET, buildRequestURL(studyCourse, fromWhen, toWhen), null, null);

        this.fromWhen = fromWhen;
        this.toWhen = toWhen;
        this.studyCourse = studyCourse;
        this.listener = listener;

        setRetryPolicy(retryPolicy);
    }

    protected void deliverResponse(String response) {
        onResponse(response);
    }

    private static String buildRequestURL(StudyCourse studyCourse, Calendar from, Calendar to) {
        return String.format(
                Locale.CANADA,
                "http://webapps.unitn.it/Orari/it/Web/AjaxEventi/c/%d-%d/agendaWeek?start=%d&end=%d",
                studyCourse.getCourse(),
                studyCourse.getYear(),
                from.getTimeInMillis() / 1000,
                to.getTimeInMillis() / 1000
        );
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        listener.onErrorHappened(error);
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

            onRequestSuccessful.dispatch(lessonsSet);
            listener.onLessonsLoaded(lessonsSet);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onParsingErrorHappened(e);
        } finally {
            onRequestTerminated.dispatch();
        }
    }
}
