package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import android.support.annotation.NonNull;

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
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
        return generateExtraRequest(listener);
    }

    @NonNull
    private ExtraLessonsRequest generateExtraRequest(LessonsLoadingListener listener) {
        return new ExtraLessonsRequest(this, extraCourse, listener);
    }

    @Override
    public IRequest generateOneTimeRequest(LessonsLoadingListener listener) {
        ExtraLessonsRequest request = generateExtraRequest(listener);
        request.setCacheCheckEnabled(false);
        request.setRetrialsEnabled(false);

        return request;
    }


}
