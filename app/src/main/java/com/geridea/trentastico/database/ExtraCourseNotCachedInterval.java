package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.network.request.ExtraLessonsRequest;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.utils.time.WeekInterval;

public class ExtraCourseNotCachedInterval extends NotCachedInterval {

    private final ExtraCourse extraCourse;

    public ExtraCourseNotCachedInterval(WeekInterval interval, ExtraCourse extraCourse) {
        super(interval.getStartCopy(), interval.getEndCopy());
        this.extraCourse = extraCourse;
    }

    @Override
    public IRequest generateRequest(LessonsLoadingListener listener) {
        return new ExtraLessonsRequest(this, listener, extraCourse);
    }


}
