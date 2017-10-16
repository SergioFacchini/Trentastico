package com.geridea.trentastico

import org.junit.Test


/*
 * Created with ♥ by Slava on 16/10/2017.
 */
class KotlinTest {

    @Test
    fun testMultipleIns() {
        var a = 5
        var b = 6
        var c = 7

        when (a) {
            in 1..6 -> {
                b = 6
            }
            in 4..8 -> {
                c = 7
            }
        }

        assert(b == 6)
        assert(c == 7)
    }

}