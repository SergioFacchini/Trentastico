package com.geridea.trentastico.model;

import com.geridea.trentastico.model.cache.CachedLesson;
import com.geridea.trentastico.network.request.LessonsDiffResult;
import com.geridea.trentastico.utils.NumbersUtils;
import com.geridea.trentastico.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.geridea.trentastico.utils.time.CalendarUtils.getDebuggableToday;

public class LessonSchedule implements Serializable {
    private final long id;
    private final String room;
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

        //TODO: check if to keep this code or not.
//        int cachedI = 0, fetchedI = 0;
//        int stop = Math.min(fetchedLessons.size(), cachedLessons.size());
//        while(Math.max(cachedI, fetchedI) == stop){
//            LessonSchedule cached  = cachedLessons .get(cachedI);
//            LessonSchedule fetched = fetchedLessons.get(fetchedI);
//
//            if(cached.equals(fetched)){
//                //This lesson did not change
//                cachedI++;
//                fetchedI++;
//            } else if(cached.getId() == fetched.getId()) {
//                //We've got the same event, but it has some details changed
//                differenceListener.onStudyCourseLessonChanged(cached, fetched);
//
//                cachedI++;
//                fetchedI++;
//            } else {
//                //The lessons differ, two things might have happened:
//                //* A new lesson was added before the one that's equal to ours
//                //* The lesson was removed
//
//                //Checking if our lessons still exists:
//                boolean foundCachedLesson = false;
//                int fetchedIWithId;
//                for(fetchedIWithId = fetchedI+1;
//                    fetchedIWithId<fetchedLessons.size() && !foundCachedLesson;
//                    fetchedIWithId++){
//
//                    LessonSchedule futureFetched = fetchedLessons.get(fetchedIWithId);
//                    if (cached.getId() == futureFetched.getId()) {
//                        //We found our lesson some lessons away. This means that new lessons were
//                        //inserted
//                        foundCachedLesson = true;
//                    }
//                }
//
//                if (foundCachedLesson) {
//                    //We found our cached lesson; all the lessons in between have to be considered
//                    //new
//                    for(int i = fetchedI+1; i<fetchedIWithId; i++){
//                        differenceListener.onStudyLessonAdded(fetchedLessons.get(i));
//                        fetchedI++;
//                    }
//
//                    cachedI++;
//                } else {
//                    //We didn't find the missing lesson. This means that the lesson has been removed
//                    differenceListener.onStudyLessonRemoved(cached);
//                    cachedI++;
//                }
//
//            }
//        }
//
//        if(cachedI > fetchedLessons.size()){
//            //We have some unprocessed cached lessons; these lessons have been removed
//            for(; cachedI<cachedLessons.size(); cachedI++){
//                differenceListener.onStudyLessonRemoved(cachedLessons.get(cachedI));
//            }
//        } else if(cachedI < fetchedLessons.size()) {
//            //We have some lessons that were added after the cache
//            for(; fetchedI<cachedLessons.size(); fetchedI++){
//                differenceListener.onStudyLessonAdded(fetchedLessons.get(fetchedI));
//            }
//        }
    }

    private boolean isMeaningfullyEqualTo(LessonSchedule that) {
        if (id         != that.id)         return false;
        if (startsAt   != that.startsAt)   return false;
        if (endsAt != that.endsAt) return false;
        if (!room   .equals(that.room))    return false;
        if (!subject.equals(that.subject)) return false;
        return fullDescription.equals(that.fullDescription);
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
}
