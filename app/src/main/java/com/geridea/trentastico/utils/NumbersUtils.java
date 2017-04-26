package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

public class NumbersUtils {

    public static int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static boolean isOneOf(int needle, int... haystack) {
        for (int element : haystack) {
            if (element == needle) {
                return true;
            }
        }

        return false;
    }
}
