package trentastico.geridea.com.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import java.util.ArrayList;
import java.util.Calendar;

import trentastico.geridea.com.trentastico.model.LessonSchedule;
import trentastico.geridea.com.trentastico.model.LessonType;
import trentastico.geridea.com.trentastico.model.LessonsSet;
import trentastico.geridea.com.trentastico.model.StudyCourse;

/**
 * Identifies a contiguous interval of cached events.
 */
public class CachedLessonsSet extends LessonsSet {

    private StudyCourse course;
    private Calendar from, to;
    private Integer lessonType = null;

    private Calendar whenWasCached;

    public CachedLessonsSet() {

    }

    public CachedLessonsSet(StudyCourse course, Calendar from, Calendar to, Integer lessonType) {
        this.course = course;
        this.from = from;
        this.to = to;
        this.lessonType = lessonType;
    }

    public StudyCourse getCourse() {
        return course;
    }

    public void setCourse(StudyCourse course) {
        this.course = course;
    }

    public Calendar getFrom() {
        return from;
    }

    public void setFrom(Calendar from) {
        this.from = from;
    }

    public Calendar getTo() {
        return to;
    }

    public void setTo(Calendar to) {
        this.to = to;
    }

    public Integer getLessonType() {
        return lessonType;
    }

    public void setLessonType(Integer lessonType) {
        this.lessonType = lessonType;
    }

    public void addLessonSchedules(ArrayList<LessonSchedule> lessons) {
        scheduledLessons.addAll(lessons);
    }

    public void addCachedLessonTypes(ArrayList<CachedLessonType> cachedLessonTypes) {
        for (CachedLessonType cachedLessonType : cachedLessonTypes) {
            addLessonType(new LessonType(cachedLessonType));
        }
    }
}
