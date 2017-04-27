package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 27/04/2017.
 */

import com.geridea.trentastico.BuildConfig;

public class DebugUtils {
    public static String computeVersionName() {
        return String.format("%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }
}
