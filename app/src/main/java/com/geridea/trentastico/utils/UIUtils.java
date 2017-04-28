package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 30/03/2017.
 */

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.Toast;

import com.geridea.trentastico.Config;

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


    public static void showToastOnMainThread(final Context context, final String message) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showToastIfInDebug(Context context, String message) {
        if (Config.DEBUG_MODE) {
            showToastOnMainThread(context, message);
        }
    }

    public static int convertDpToPixels(float dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int convertSpToPixels(float sp, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics);
    }

}
