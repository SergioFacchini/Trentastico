package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.listener.LessonsDifferenceListener;
import com.geridea.trentastico.utils.time.WeekInterval;

public abstract class CachedInterval extends WeekInterval {

    public CachedInterval(WeekInterval anotherInterval) {
        super(anotherInterval.getStartCopy(), anotherInterval.getEndCopy());
    }

    public abstract IRequest generateDiffRequest(LessonsDifferenceListener listener);

}
