package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 30/03/2017.
 */

import android.os.Handler;
import android.os.Looper;

public class UIUtils {

    /**
     * Runs a runnable on the UI thread.
     *
     * @param runnable the runnable to run on the UI thread
     */
    public static void runOnMainThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }


}
