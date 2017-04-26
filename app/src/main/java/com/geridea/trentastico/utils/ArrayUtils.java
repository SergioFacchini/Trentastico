package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

public class ArrayUtils {

    public static boolean isOneOf(int needle, int... haystack) {
        for (int element : haystack) {
            if (element == needle) {
                return true;
            }
        }

        return false;
    }

    public static <T extends Enum<T>> boolean isOneOf(Enum<T> needle, Enum<T>... haystack){
        for (Enum<T> elem : haystack) {
            if (needle == elem) {
                return true;
            }
        }

        return false;
    }

}
