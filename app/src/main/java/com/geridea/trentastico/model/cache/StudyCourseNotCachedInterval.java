package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import android.support.annotation.NonNull;

import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.model.cache.NotCachedInterval;
import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.StudyCourseLessonsRequest;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;

public class StudyCourseNotCachedInterval extends NotCachedInterval {

    public StudyCourseNotCachedInterval(WeekTime start, WeekTime end) {
        super(start, end);

    }

    public StudyCourseNotCachedInterval(WeekInterval interval) {
        super(interval.getStartCopy(), interval.getEndCopy());
    }

    @Override
    public IRequest generateRequest(LessonsLoadingListener listener) {
        return generateStudyRequest(listener);
    }

    @NonNull
    private StudyCourseLessonsRequest generateStudyRequest(LessonsLoadingListener listener) {
        StudyCourse studyCourse = AppPreferences.getStudyCourse();
        return new StudyCourseLessonsRequest(this, studyCourse, listener);
    }

    @Override
    public IRequest generateOneTimeRequest(LessonsLoadingListener listener) {
        StudyCourseLessonsRequest request = generateStudyRequest(listener);

        request.setCacheCheckEnabled(false);
        request.setRetrialsEnabled(false);

        return request;
    }

}
