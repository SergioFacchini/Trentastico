package com.geridea.trentastico.model;

import com.geridea.trentastico.model.cache.CachedLesson;
import com.geridea.trentastico.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LessonSchedule {
    private final long id; //Seems to be an unique identifier
    private final String room;
    private final String subject;
    private final long startsAt;
    private final long finishesAt;
    private final String fullDescription;
    private final int color;
    private final long lessonTypeId;

    public LessonSchedule(long id, String room, String subject, long startsAt, long finishesAt, String fullDescription, int color, long lessonTypeId) {
        this.id = id;
        this.room = room;
        this.subject = subject;
        this.startsAt = startsAt;
        this.finishesAt = finishesAt;
        this.fullDescription = fullDescription;
        this.color = color;
        this.lessonTypeId = lessonTypeId;
    }

    public LessonSchedule(CachedLesson cachedLesson, int color) {
        this(
            cachedLesson.getLesson_id(),
            cachedLesson.getRoom(),
            cachedLesson.getSubject(),
            cachedLesson.getStarts_at_ms(),
            cachedLesson.getFinishes_at_ms(),
            cachedLesson.getDescription(),
            color,
            cachedLesson.getTeaching_id()
        );

    }

    public long getId() {
        return id;
    }

    public String getRoom() {
        return room;
    }

    public String getSubject() {
        return subject;
    }

    public long getStartsAt() {
        return startsAt;
    }

    public long getFinishesAt() {
        return finishesAt;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public int getColor() {
        return color;
    }

    public long getLessonTypeId() {
        return lessonTypeId;
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

        long id = Long.valueOf(json.getString("url").substring(1)); //url: "#123453"

        String room    = getRoomFromTitle(titleToParse);
        String subject = getSubjectFromTitle(titleToParse);

        long start = json.getLong("start")*1000;
        long end   = json.getLong("end")*1000;

        long teachingId = json.getInt("id");

        int color = LessonType.getColorFromCSSStyle(json.getString("className"));

        return new LessonSchedule(id, room, subject, start, end, titleToParse, color, teachingId);
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

    public boolean hasLessonType(LessonType lessonType) {
        return getLessonTypeId() == lessonType.getId();
    }

    public boolean matchesPartitioningType(PartitioningType partitioningType) {
        return StringUtils.containsMatchingRegex(partitioningType.getRegex(), getFullDescription());
    }

    public boolean matchesAnyOfPartitioningCases(ArrayList<PartitioningCase> partitionings) {
        for (PartitioningCase partitioning : partitionings) {
            if (fullDescription.contains(partitioning.getCase())) {
                return true;
            }
        }

        return false;
    }
}
