package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.utils.time.WeekInterval

class StudyCourseNotCachedInterval(interval: WeekInterval) : NotCachedInterval(interval.startCopy, interval.endCopy) {

    override fun launchLoading(controller: LessonsController, listener: LessonsLoadingListener) {
        controller.sendStudyCourseLoadingRequest(this, listener)
    }

    override fun launchLoadingOneTime(controller: LessonsController, listener: LessonsLoadingListener) {
        controller.sendStudyCourseLoadingRequestOneTime(this, listener)
    }

}
