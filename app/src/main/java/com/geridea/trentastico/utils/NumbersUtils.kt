package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

object NumbersUtils {

    fun compare(x: Long, y: Long): Int {
        return if (x < y) -1 else if (x == y) 0 else 1
    }

}
