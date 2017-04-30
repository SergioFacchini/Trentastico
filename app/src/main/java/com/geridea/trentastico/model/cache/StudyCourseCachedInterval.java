package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class StudyCourseCachedInterval extends CachedInterval {

    private final ArrayList<LessonSchedule> cachedLessons;
    private final StudyCourse studyCourse;

    public StudyCourseCachedInterval(WeekInterval interval, StudyCourse studyCourse, ArrayList<LessonSchedule> cachedLessons) {
        super(interval);
        this.cachedLessons = cachedLessons;

        this.studyCourse = studyCourse;
    }

    @Override
    public void launchDiffRequest(LessonsController controller, LessonsDifferenceListener listener) {
        controller.diffStudyCourseLessons(this, studyCourse, cachedLessons, listener);
    }
}
