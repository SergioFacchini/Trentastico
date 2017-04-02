package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.request.ExtraCourseLessonsDiffRequest;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.StudyCourseLessonsDiffRequest;
import com.geridea.trentastico.network.request.listener.LessonsDifferenceListener;
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
    public IRequest generateDiffRequest(LessonsDifferenceListener listener) {
        return new ExtraCourseLessonsDiffRequest(this, extraCourse, cachedLessons, listener);
    }

}
