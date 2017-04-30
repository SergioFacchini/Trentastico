package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.utils.time.WeekInterval;

public class ExtraCourseNotCachedInterval extends NotCachedInterval {

    private final ExtraCourse extraCourse;

    public ExtraCourseNotCachedInterval(WeekInterval interval, ExtraCourse extraCourse) {
        super(interval.getStartCopy(), interval.getEndCopy());
        this.extraCourse = extraCourse;
    }

    @Override
    public void launchLoading(LessonsController controller, LessonsLoadingListener listener) {
        controller.sendExtraCourseLoadingRequest(this, extraCourse, listener);
    }

    @Override
    public void launchLoadingOneTime(LessonsController controller, LessonsLoadingListener listener) {
        controller.sendExtraCourseLoadingRequestOneTime(this, extraCourse, listener);
    }

}
