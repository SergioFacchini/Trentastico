package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

object ArrayUtils {

    fun <T : Enum<T>> isOneOf(needle: Enum<T>, vararg haystack: Enum<T>): Boolean =
            haystack.any { needle === it }

}

