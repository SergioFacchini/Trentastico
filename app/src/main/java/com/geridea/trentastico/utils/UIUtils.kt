package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 30/03/2017.
 */

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.Toast

import com.geridea.trentastico.Config

object UIUtils {

    /**
     * Runs a runnable on the UI thread.
     *
     * @param runnable the runnable to run on the UI thread
     */
    fun runOnMainThread(runnable: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(runnable)
    }


    fun showToastOnMainThread(context: Context, message: String) {
        runOnMainThread({
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        })
    }

    fun showToastIfInDebug(context: Context, message: String) {
        if (Config.DEBUG_MODE) {
            showToastOnMainThread(context, message)
        }
    }

    fun convertDpToPixels(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }

    fun convertSpToPixels(sp: Float, context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics).toInt()
    }

}
