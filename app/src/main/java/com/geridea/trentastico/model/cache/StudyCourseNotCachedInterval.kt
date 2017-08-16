package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.utils.time.WeekInterval
import com.geridea.trentastico.utils.time.WeekTime

class StudyCourseNotCachedInterval : NotCachedInterval {

    constructor(start: WeekTime, end: WeekTime) : super(start, end) {

    }

    override fun launchLoading(controller: LessonsController, listener: LessonsLoadingListener) {
        controller.sendStudyCourseLoadingRequest(this, listener)
    }

    override fun launchLoadingOneTime(controller: LessonsController, listener: LessonsLoadingListener) {
        controller.sendStudyCourseLoadingRequestOneTime(this, listener)
    }

    constructor(interval: WeekInterval) : super(interval.startCopy, interval.endCopy) {}

}
