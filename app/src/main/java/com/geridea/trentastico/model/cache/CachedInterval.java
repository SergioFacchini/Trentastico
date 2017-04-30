package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.network.controllers.listener.LessonsDifferenceListener;
import com.geridea.trentastico.utils.time.WeekInterval;

public abstract class CachedInterval extends WeekInterval {

    public CachedInterval(WeekInterval anotherInterval) {
        super(anotherInterval.getStartCopy(), anotherInterval.getEndCopy());
    }

    /**
     * Launches on the controller a diff request pertinent to the type of this cached interval.
     * @param controller the controller to launch the request from
     * @param listener the listener to apply to the diff request
     */
    public abstract void launchDiffRequest(LessonsController controller, LessonsDifferenceListener listener);
}
