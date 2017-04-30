package com.geridea.trentastico.network.controllers.listener;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.threerings.signals.Signal1;

public class WaitForDownloadListenerToSignalAdapter extends WaitForDownloadLessonListener {

    private final Signal1<Boolean> signalToNotify;

    public WaitForDownloadListenerToSignalAdapter(Signal1<Boolean> signalToNotify) {
        this.signalToNotify = signalToNotify;
    }

    @Override
    public void onFinish(boolean withSuccess) {
        signalToNotify.dispatch(withSuccess);
    }
}
