package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.EnqueueableOperation;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.network.requests.LessonsRequest;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;

public class StudyCourseNotCachedInterval extends NotCachedInterval {

    public StudyCourseNotCachedInterval(WeekTime start, WeekTime end) {
        super(start, end);

    }

    public StudyCourseNotCachedInterval(WeekInterval interval) {
        super(interval.getStart(), interval.getEnd());
    }

    @Override
    public EnqueueableOperation generateRequest(LessonsLoadingListener listener) {
        StudyCourse studyCourse = AppPreferences.getStudyCourse();
        return new LessonsRequest(this, studyCourse, listener);
    }

}
