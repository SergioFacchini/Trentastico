package com.geridea.trentastico.gui.views.requestloader;


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.utils.time.WeekInterval;

public class LoadingFromCacheMessage extends AbstractTextMessage {

    private WeekInterval intervalToLoad;

    public LoadingFromCacheMessage(WeekInterval intervalToLoad) {
        super(RequestLoaderView.LOADING_FROM_CACHE_OPERATION_ID);

        this.intervalToLoad = intervalToLoad;
    }

    @Override
    public String getText() {
        return String.format("Sto cercando gli orari dal %s...", formatFromToString(intervalToLoad));
    }
}
