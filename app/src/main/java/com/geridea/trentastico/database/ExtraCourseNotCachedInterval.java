package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.network.EnqueueableOperation;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.network.requests.ExtraCourseLessonsRequest;
import com.geridea.trentastico.utils.time.WeekInterval;

public class ExtraCourseNotCachedInterval extends NotCachedInterval {

    private final ExtraCourse extraCourse;

    public ExtraCourseNotCachedInterval(WeekInterval interval, ExtraCourse extraCourse) {
        super(interval.getStart(), interval.getEnd());
        this.extraCourse = extraCourse;
    }

    @Override
    public EnqueueableOperation generateRequest(LessonsLoadingListener listener) {
        return new ExtraCourseLessonsRequest(this, listener, extraCourse);
    }


}
