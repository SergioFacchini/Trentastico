package com.geridea.trentastico.gui.views.requestloader


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.utils.time.WeekInterval

class LoadingFromCacheMessage(private val intervalToLoad: WeekInterval) : AbstractTextMessage(RequestLoaderView.LOADING_FROM_CACHE_OPERATION_ID) {

    override val text: String
        get() = String.format("Sto cercando gli orari dal %s...", formatFromToString(intervalToLoad))
}
