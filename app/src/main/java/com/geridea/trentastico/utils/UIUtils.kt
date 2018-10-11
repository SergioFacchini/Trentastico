package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 30/03/2017.
 */

import android.content.ClipData
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast

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


    fun showToastOnMainThread(context: Context, message: String) = runOnMainThread({
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    })

    fun showToastIfInDebug(context: Context, message: String) {
        if (IS_IN_DEBUG_MODE) {
            showToastOnMainThread(context, message)
        }
    }

    fun convertSpToPixels(sp: Float, context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics).toInt()
    }

}

/**
 * Sets the text if the given parameter is not null and not empty, otherwise the [TextView] is
 * hidden.
 */
fun TextView.setTextOrHideIfEmpty(text: String?) {
    if (text.isNullOrBlank()) {
        visibility = View.GONE
    } else {
        this.text = text
    }
}


fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText("Copied Text", text)
}
