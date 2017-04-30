package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.util.ArrayList;

public class ExtraCourseCachedInterval extends CachedInterval {

    private final ArrayList<LessonSchedule> cachedLessons;
    private final ExtraCourse extraCourse;

    public ExtraCourseCachedInterval(WeekInterval interval, ExtraCourse extraCourse, ArrayList<LessonSchedule> cachedLessons) {
        super(interval);

        this.cachedLessons = cachedLessons;
        this.extraCourse = extraCourse;
    }

    @Override
    public void launchDiffRequest(LessonsController controller, LessonsDifferenceListener listener) {
        controller.diffExtraCourseLessons(this, extraCourse, cachedLessons, listener);
    }

}
