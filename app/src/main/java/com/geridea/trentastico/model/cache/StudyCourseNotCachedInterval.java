package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;

public class StudyCourseNotCachedInterval extends NotCachedInterval {

    public StudyCourseNotCachedInterval(WeekTime start, WeekTime end) {
        super(start, end);

    }

    @Override
    public void launchLoading(LessonsController controller, LessonsLoadingListener listener) {
        controller.sendStudyCourseLoadingRequest(this, listener);
    }

    @Override
    public void launchLoadingOneTime(LessonsController controller, LessonsLoadingListener listener) {
        controller.sendStudyCourseLoadingRequestOneTime(this, listener);
    }

    public StudyCourseNotCachedInterval(WeekInterval interval) {
        super(interval.getStartCopy(), interval.getEndCopy());
    }

}
