package com.geridea.trentastico.model.cache;


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import com.geridea.trentastico.network.request.listener.LessonsLoadingListener;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.geridea.trentastico.utils.time.WeekTime;

public abstract class NotCachedInterval extends WeekInterval {

    public NotCachedInterval(WeekTime start, WeekTime end) {
        super(start, end);
    }

    public abstract IRequest generateRequest(LessonsLoadingListener listener);

    public abstract IRequest generateOneTimeRequest(LessonsLoadingListener listener);

}
