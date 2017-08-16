package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

object ArrayUtils {

    fun isOneOf(needle: Int, vararg haystack: Int): Boolean {
        for (element in haystack) {
            if (element == needle) {
                return true
            }
        }

        return false
    }

    fun <T : Enum<T>> isOneOf(needle: Enum<T>, vararg haystack: Enum<T>): Boolean {
        for (elem in haystack) {
            if (needle === elem) {
                return true
            }
        }

        return false
    }

}
