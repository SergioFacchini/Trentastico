package com.geridea.trentastico.network.controllers.listener


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.threerings.signals.Signal1

class WaitForDownloadListenerToSignalAdapter(private val signalToNotify: Signal1<Boolean>) : WaitForDownloadLessonListener() {

    override fun onFinish(withSuccess: Boolean) {
        signalToNotify.dispatch(withSuccess)
    }
}
