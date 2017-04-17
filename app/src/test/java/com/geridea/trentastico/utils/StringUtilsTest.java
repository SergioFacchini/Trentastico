package com.geridea.trentastico.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/*
 * Created with ♥ by Slava on 25/03/2017.
 */
public class StringUtilsTest {

    @Test
    public void testRegexs() throws Exception {
        assertTrue(StringUtils.containsMatchingRegex("happy", "unhappy"));

    }

    @Test
    public void testPositionFormat() throws Exception {
        assertPositionFormat("Hello World!", "Hello {0}!", "World");
        assertPositionFormat("Hello Bob!", "Hello {0}!", "Bob");
        assertPositionFormat("Hello Bob! My name is Bob too!", "Hello {0}! My name is {0} too!", "Bob");
        assertPositionFormat("Let's count: 1 2 3", "Let's count: {0} {1} {2}", 1, 2, 3);
        assertPositionFormat("Let's count: 3 2 1", "Let's count: {2} {1} {0}", 1, 2, 3);
    }

    private void assertPositionFormat(String expected, String format, Object... items) {
        assertEquals(expected, StringUtils.positionFormat(format, items));
    }

}