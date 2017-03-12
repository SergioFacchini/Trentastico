package trentastico.geridea.com.trentastico.activities.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LessonSchedule {
    private String room;
    private String subject;
    private long startsAt;
    private long finishesAt;
    private String fullDescription;
    private int color;
    private int teachingId;

    public LessonSchedule(String room, String subject, long startsAt, long finishesAt, String fullDescription, int color, int teachingId) {
        this.room = room;
        this.subject = subject;
        this.startsAt = startsAt;
        this.finishesAt = finishesAt;
        this.fullDescription = fullDescription;
        this.color = color;
        this.teachingId = teachingId;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long startsAt) {
        this.startsAt = startsAt;
    }

    public long getFinishesAt() {
        return finishesAt;
    }

    public void setFinishesAt(long finishesAt) {
        this.finishesAt = finishesAt;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public int getColor() {
        return color;
    }

    public int getTeachingId() {
        return teachingId;
    }

    public Calendar getStartCal() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getStartsAt());
        return calendar;
    }

    public Calendar getEndCal() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getFinishesAt());
        return calendar;
    }

    public String getSynopsis() {
        String room = getRoom();

        SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm");
        String startTime = hhmm.format(getStartCal().getTime());
        String endTime   = hhmm.format(getEndCal()  .getTime());

        return String.format("%s  %s - %s", room, startTime, endTime);
    }


    public static LessonSchedule fromJson(JSONObject json) throws JSONException {
        String titleToParse = json.getString("title");

        String room    = getRoomFromTitle(titleToParse);
        String subject = getSubjectFromTitle(titleToParse);

        long start = json.getLong("start")*1000;
        long end   = json.getLong("end")*1000;

        int teachingId = json.getInt("id");

        int color = LessonType.getColorFromCSSStyle(json.getString("className"));

        return new LessonSchedule(room, subject, start, end, titleToParse, color, teachingId);
    }

    private static String getSubjectFromTitle(String titleToParse) {
        Pattern pattern = Pattern.compile("^(.+)\\n");
        Matcher matcher = pattern.matcher(titleToParse);
        matcher.find();
        return matcher.group(1);
    }

    private static String getRoomFromTitle(String titleToParse) {
        Pattern pattern = Pattern.compile("\\[(.+)\\]$");
        Matcher matcher = pattern.matcher(titleToParse);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

}
