package com.geridea.trentastico.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


/*
 * Created with ♥ by Slava on 25/03/2017.
 */
public class StringUtilsTest {

    @Test
    public void testRegexs() throws Exception {

        assertTrue(StringUtils.containsMatchingRegex("happy", "unhappy"));

    }

}