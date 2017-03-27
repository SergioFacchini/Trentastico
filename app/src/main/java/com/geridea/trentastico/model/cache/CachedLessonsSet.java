package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

import com.geridea.trentastico.database.NotCachedInterval;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class CachedLessonsSet extends LessonsSet {

    private ArrayList<NotCachedInterval> missingIntervals = new ArrayList<>();
    private ArrayList<CachedPeriod> cachedPeriods = new ArrayList<>();

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

    /**
     * @return the intervals that we were unable to load from cache.
     */
    public ArrayList<NotCachedInterval> getMissingIntervals() {
        return missingIntervals;
    }

    public void addCachedPeriod(CachedPeriod cachedPeriod) {
        cachedPeriods.add(cachedPeriod);
    }

    public ArrayList<WeekInterval> getCachedIntervals() {
        ArrayList<WeekInterval> cachedIntervals = new ArrayList<>();
        for (CachedPeriod cachedPeriod : cachedPeriods) {
            cachedIntervals.add(cachedPeriod.getPeriod());
        }

        return cachedIntervals;
    }
}
