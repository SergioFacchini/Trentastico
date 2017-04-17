package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.request.listener.DiffCompletedListener;
import com.geridea.trentastico.network.request.listener.LessonsDifferenceListener;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class StudyCourseLessonsDiffRequest extends StudyCourseLessonsRequest  {

    private final ArrayList<LessonSchedule> cachedLessons;
    private final LessonsDifferenceListener differenceListener;

    public StudyCourseLessonsDiffRequest(
            WeekInterval interval, StudyCourse course, ArrayList<LessonSchedule> cachedLessons,
            LessonsDifferenceListener differenceListener) {

        super(interval, course, new DiffCompletedListener(differenceListener));

        this.cachedLessons = cachedLessons;
        this.differenceListener = differenceListener;

        setRetrialsEnabled(false);
        setCacheCheckEnabled(false);
    }

    @Override
    protected void onLessonsSetAvailable(LessonsSet lessonsSet) {
        ArrayList<LessonSchedule> fetchedLessons = new ArrayList<>(lessonsSet.getScheduledLessons());

        //Do not compare what we filtered
        LessonSchedule.filterLessons(fetchedLessons);
        LessonSchedule.filterLessons(cachedLessons);

        differenceListener.onDiffResult(LessonSchedule.diffLessons(cachedLessons, fetchedLessons));
    }

}
