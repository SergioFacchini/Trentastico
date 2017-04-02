package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.ExtraCoursesList;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class CachedLessonsSet extends LessonsSet {

    private ArrayList<NotCachedInterval> missingIntervals = new ArrayList<>();
    private ArrayList<CachedInterval> cachedIntervals = new ArrayList<>();

    public void addCachedLessonTypes(ArrayList<CachedLessonType> cachedLessonTypes) {
        for (CachedLessonType cachedLessonType : cachedLessonTypes) {

            boolean isVisible = !AppPreferences.hasLessonTypeWithIdHidden(cachedLessonType.getLesson_type_id());
            addLessonType(new LessonType(cachedLessonType, isVisible));
        }
    }

    public void addMissingIntervals(ArrayList<NotCachedInterval> missingIntervals) {
        this.missingIntervals.addAll(missingIntervals);
    }

    public boolean hasMissingIntervals() {
        return !missingIntervals.isEmpty();
    }

    public boolean wereSomeLessonsFoundInCache() {
        return !scheduledLessons.isEmpty();
    }

    public boolean isIntervalPartiallyOrFullyCached(WeekInterval intervalToCheck) {
        for (WeekInterval cachedPeriod : cachedIntervals) {
            if(cachedPeriod.overlaps(intervalToCheck)){
                return true;
            }
        }

        return false;
    }

    /**
     * @return the intervals that we were unable to load from cache.
     */
    public ArrayList<NotCachedInterval> getMissingIntervals() {
        return missingIntervals;
    }

    public void addCachedPeriod(CachedInterval cachedPeriod) {
        cachedIntervals.add(cachedPeriod);
    }

    public ArrayList<WeekInterval> getCachedWeekIntervals() {
        ArrayList<WeekInterval> cachedIntervals = new ArrayList<>();
        for (WeekInterval cachedPeriod : this.cachedIntervals) {
            cachedIntervals.add(cachedPeriod);
        }

        return cachedIntervals;
    }

    public ArrayList<CachedInterval> getCachedIntervals() {
        return cachedIntervals;
    }

    private ArrayList<LessonSchedule> getCachedCourseLessons(CachedPeriod cachedPeriod) {
        ExtraCoursesList extraCourses = AppPreferences.getExtraCourses();
        ArrayList<LessonSchedule> lessonsToReturn = new ArrayList<>();
        for (LessonSchedule lesson : getScheduledLessons()) {
            if(!extraCourses.isAnExtraLesson(lesson) &&
                    cachedPeriod.canContainStudyLesson(lesson)){
                lessonsToReturn.add(lesson);
            }
        }

        return lessonsToReturn;
    }

    private ArrayList<LessonSchedule> getCachedExtraLessons(CachedPeriod cachedPeriod, ExtraCourse extraCourse) {
        ArrayList<LessonSchedule> lessonsToReturn = new ArrayList<>();
        for (LessonSchedule lesson : getScheduledLessons()) {
            if(cachedPeriod.canContainExtraLesson(lesson, extraCourse)){
                lessonsToReturn.add(lesson);
            }
        }

        return lessonsToReturn;
    }
}
