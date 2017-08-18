package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 27/04/2017.
 */

import com.geridea.trentastico.BuildConfig

object DebugUtils {
    fun computeVersionName(): String = String.format("%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
}
