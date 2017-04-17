package com.geridea.trentastico.model;

import com.geridea.trentastico.model.cache.CachedLesson;
import com.geridea.trentastico.network.request.LessonsDiffResult;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.NumbersUtils;
import com.geridea.trentastico.utils.StringUtils;
import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.CalendarUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.geridea.trentastico.utils.time.CalendarUtils.getDebuggableToday;

public class LessonSchedule implements Serializable {
    private final long id;
    private String room;
    private final String subject;
    private final long startsAt;
    private final long endsAt;
    private final String fullDescription;
    private final int color;
    private final long lessonTypeId;

    public LessonSchedule(long id, String room, String subject, long startsAt, long endsAt, String fullDescription, int color, long lessonTypeId) {
        this.id = id;
        this.room = room;
        this.subject = subject;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.fullDescription = fullDescription;
        this.color = color;
        this.lessonTypeId = lessonTypeId;
    }

    public LessonSchedule(CachedLesson cachedLesson) {
        this(
            cachedLesson.getLesson_id(),
            cachedLesson.getRoom(),
            cachedLesson.getSubject(),
            cachedLesson.getStarts_at_ms(),
            cachedLesson.getFinishes_at_ms(),
            cachedLesson.getDescription(),
            cachedLesson.getColor(),
            cachedLesson.getTeaching_id()
        );

    }

    public static LessonsDiffResult diffLessons(ArrayList<LessonSchedule> cachedLessons, ArrayList<LessonSchedule> fetchedLessons) {
        LessonsDiffResult diffResult = new LessonsDiffResult();

        sortByStartDateOrId(fetchedLessons);
        sortByStartDateOrId(cachedLessons);

        ArrayList<LessonSchedule> fetchedNotCachedLesson = new ArrayList<>(fetchedLessons);
        for (LessonSchedule cached : cachedLessons) {

            boolean cacheLessonFound = false;
            for (LessonSchedule fetched : fetchedLessons) {
                if (cached.getId() == fetched.getId()) {
                    //We found the lesson
                    fetchedNotCachedLesson.remove(fetched);

                    if(!cached.isMeaningfullyEqualTo(fetched)){
                        diffResult.addChangedLesson(cached, fetched);
                    }

                    cacheLessonFound = true;
                    break;
                }
            }

            if (!cacheLessonFound) {
                diffResult.addRemovedLesson(cached);
            }
        }

        for (LessonSchedule lessonNotInCache : fetchedNotCachedLesson) {
            diffResult.addAddedLesson(lessonNotInCache);
        }

        return diffResult;
    }

    public static CourseAndYear findCourseAndYearForLesson(LessonSchedule lesson) {
        for (ExtraCourse extraCourse : AppPreferences.getExtraCourses()) {
            if (extraCourse.getLessonTypeId() == lesson.getLessonTypeId()) {
                return extraCourse.getCourseAndYear();
            }
        }

        return AppPreferences.getStudyCourse().getCourseAndYear();
    }

    public static ArrayList<LessonSchedule> getLessonsOfType(LessonType lessonType, Collection<LessonSchedule> lessons) {
        ArrayList<LessonSchedule> toReturn = new ArrayList<>();

        for (LessonSchedule lessonSchedule : lessons) {
            if (lessonSchedule.getLessonTypeId() == lessonType.getId()) {
                toReturn.add(lessonSchedule);
            }
        }

        return toReturn;
    }

    public static void filterLessons(Collection<LessonSchedule> lessonsToFilter) {
        Iterator<LessonSchedule> lessonsIterator = lessonsToFilter.iterator();

        while(lessonsIterator.hasNext()){
            LessonSchedule lesson = lessonsIterator.next();

            boolean wasRemoved = false;

            //Checking if is filtered because of the lesson type
            for (Long lessonTypeIdToHide : AppPreferences.getLessonTypesIdsToHide()) {
                if (lesson.getLessonTypeId() == lessonTypeIdToHide) {
                    lessonsIterator.remove();
                    wasRemoved = true;
                    break;
                }
            }

            //Checking if is filtered because of the partitioning
            if (!wasRemoved) {
                for (String partitioning : AppPreferences.getHiddenPartitionings(lesson.getLessonTypeId())) {
                    if (lesson.hasPartitioning(partitioning)) {
                        lessonsIterator.remove();
                        break;
                    }
                }
            }
        }
    }

    private boolean isMeaningfullyEqualTo(LessonSchedule that) {
        return id == that.id
            && startsAt == that.startsAt
            && endsAt == that.endsAt
            && room.equals(that.room)
            && subject.equals(that.subject)
            && fullDescription.equals(that.fullDescription);
    }

    /**
     * @return the unique identifier of the lesson
     */
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

    public long getEndsAt() {
        return endsAt;
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
        Calendar calendar = getDebuggableToday();
        calendar.setTimeInMillis(getStartsAt());
        return calendar;
    }

    public Calendar getEndCal() {
        Calendar calendar = getDebuggableToday();
        calendar.setTimeInMillis(getEndsAt());
        return calendar;
    }

    public String getSynopsis() {
        String room = getRoom();

        SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm");
        String startTime = hhmm.format(getStartCal().getTime());
        String endTime   = hhmm.format(getEndCal()  .getTime());

        if (room.isEmpty()) {
            return String.format("%s-%s", startTime, endTime);
        } else {
            return String.format("%s-%s | %s", startTime, endTime, room);
        }
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

    public static void sortByStartDateOrId(ArrayList<LessonSchedule> lessons) {
        Collections.sort(lessons, new Comparator<LessonSchedule>() {
            @Override
            public int compare(LessonSchedule a, LessonSchedule b) {
                int compare = NumbersUtils.compare(a.getStartsAt(), b.getStartsAt());
                if (compare == 0) {
                    compare = NumbersUtils.compare(a.getId(), b.getId());
                }

                return compare;
            }
        });
    }

    @Override
    public String toString() {
        return String.format("[id: %d lessonType: %d description: %s ]", getId(), getLessonTypeId(), getFullDescription());
    }

    public int getDurationInMinutes() {
        return (int) TimeUnit.MILLISECONDS.toMinutes(endsAt - startsAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LessonSchedule that = (LessonSchedule) o;

        return id           == that.id
            && startsAt     == that.startsAt
            && endsAt       == that.endsAt
            && color        == that.color
            && lessonTypeId == that.lessonTypeId
            && room           .equals(that.room)
            && subject        .equals(that.subject)
            && fullDescription.equals(that.fullDescription);
    }

    public boolean startsBefore(long currentMillis) {
        return getStartsAt() < currentMillis;
    }

    public boolean isHeldInMilliseconds(long ms) {
        return startsAt >= ms && ms <= endsAt;
    }

    public boolean hasRoomSpecified() {
        return !room.isEmpty();
    }


    public void setRoom(String room) {
        this.room = room;
    }

    public CalendarInterval toExpandedCalendarInterval(int typeOfTime, int delta) {
        Calendar calFrom = CalendarUtils.getCalendarInitializedAs(startsAt);
        calFrom.add(typeOfTime, -delta);

        Calendar calTo = CalendarUtils.getCalendarInitializedAs(endsAt);
        calTo.add(typeOfTime, delta);

        return new CalendarInterval(calFrom, calTo);
    }

    public boolean hasPartitioning(String partitioningText) {
        return getFullDescription().contains("("+partitioningText+")");
    }
}
