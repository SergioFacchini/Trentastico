package com.geridea.trentastico.model.cache


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.utils.time.WeekInterval
import com.geridea.trentastico.utils.time.WeekTime

abstract class NotCachedInterval(start: WeekTime, end: WeekTime) : WeekInterval(start, end) {

    /**
     * Asks the controller to launch a network request that would fetch this interval. This request
     * will not make more than one loading attempt. If no more than than one attempt have to be made,
     * then [NotCachedInterval.launchLoadingOneTime]
     * is more appropriate.
     * @param controller the controller that will launch the request
     * @param listener listener to associate to the request
     */
    abstract fun launchLoading(controller: LessonsController, listener: LessonsLoadingListener)


    /**
     * Asks the controller to launch a network request that would fetch this interval. This request
     * will not make more than one loading attempt. If the loading is unsuccessful, the
     * [LessonsLoadingListener.onLoadingAborted] will be called
     * @param controller the controller that will launch the request
     * @param listener listener to associate to the request
     */
    abstract fun launchLoadingOneTime(controller: LessonsController, listener: LessonsLoadingListener)
}
