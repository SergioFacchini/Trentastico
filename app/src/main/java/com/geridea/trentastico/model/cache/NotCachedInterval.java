package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;

public abstract class NotCachedInterval extends WeekInterval {

    public NotCachedInterval(WeekTime start, WeekTime end) {
        super(start, end);
    }

    /**
     * Asks the controller to launch a network request that would fetch this interval. This request
     * will not make more than one loading attempt. If no more than than one attempt have to be made,
     * then {@link NotCachedInterval#launchLoadingOneTime(LessonsController, LessonsLoadingListener)}
     * is more appropriate.
     * @param controller the controller that will launch the request
     * @param listener listener to associate to the request
     */
    public abstract void launchLoading(LessonsController controller, LessonsLoadingListener listener);


    /**
     * Asks the controller to launch a network request that would fetch this interval. This request
     * will not make more than one loading attempt. If the loading is unsuccessful, the
     * {@link LessonsLoadingListener#onLoadingAborted(int)} will be called
     * @param controller the controller that will launch the request
     * @param listener listener to associate to the request
     */
    public abstract void launchLoadingOneTime(LessonsController controller, LessonsLoadingListener listener);
}
