package com.geridea.trentastico.utils;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertFalse;
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
    public void libraryOpeningTimeLessons() throws Exception {
        Pattern compile = Pattern.compile("(chiuso)|([0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2})");

        assertTrue(compile.matcher("chiuso").find());
        assertTrue(compile.matcher("01:11-22:11").find());
        assertFalse(compile.matcher("prova prova").find());
        assertTrue(compile.matcher("prova chiuso prova").find());

        Matcher matcher = compile.matcher("<div id=\"hp_openings_hours\"><h3 style=\"text-align:center;margin-top:5px;\">\n" +
                "    <a class=\"prev-day\" href=\"#\" onclick=\"loadDoc('prev')\" style=\"float:left;\">Prev</a>\n" +
                "    LunedÃ¬ 24 Aprile\n" +
                "    <a class=\"next-day\" href=\"#\" onclick=\"loadDoc('next')\" style=\"float:right;\">Next</a>\n" +
                "</h3>\n" +
                "    <div class=\"sede\">\n" +
                "        <span class=\"sede-title\" style=\"margin-left:10px;\">\n" +
                "            <a href=\"biblioteca-universitaria-centrale-buc\" title=\"Biblioteca Universitaria Centrale (BUC)\">Biblioteca Universitaria Centrale (BUC)</a>\n" +
                "        </span>\n" +
                "        <span class=\"sede-open-time\" style=\"float:right;margin-right:10px;\">08:00-23:45 </span>\n" +
                "    </div>\n" +
                "    <div class=\"sede\">\n" +
                "        <span class=\"sede-title\" style=\"margin-left:10px;\">\n" +
                "            <a href=\"sala-studio-di-via-verdi-8\" title=\"Sala studio di Via Verdi 8\">Sala studio di Via Verdi 8</a>\n" +
                "        </span>\n" +
                "        <span class=\"sede-open-time\" style=\"float:right;margin-right:10px;\">08:00-19:45 </span>\n" +
                "    </div>\n" +
                "    <div class=\"sede\">\n" +
                "        <span class=\"sede-title\" style=\"margin-left:10px;\">\n" +
                "            <a href=\"biblioteca-di-ingegneria\" title=\"Biblioteca di Ingegneria\">Biblioteca di Ingegneria</a>\n" +
                "        </span>\n" +
                "        <span style=\"float:right;color:#ca3538;margin-right:2.5%;\">chiuso</span>\n" +
                "    </div>\n" +
                "    <div class=\"sede\">\n" +
                "        <span class=\"sede-title\" style=\"margin-left:10px;\">\n" +
                "            <a href=\"biblioteca-di-scienze\" title=\"Biblioteca di Scienze\">Biblioteca di Scienze</a>\n" +
                "        </span>\n" +
                "        <span style=\"float:right;color:#ca3538;margin-right:2.5%;\">chiuso</span>\n" +
                "    </div>\n" +
                "    <div class=\"sede\">\n" +
                "        <span class=\"sede-title\" style=\"margin-left:10px;\">\n" +
                "            <a href=\"biblioteca-di-scienze-cognitive\" title=\"Biblioteca di Scienze Cognitive\">Biblioteca di Scienze Cognitive</a>\n" +
                "        </span>\n" +
                "        <span class=\"sede-open-time\" style=\"float:right;margin-right:10px;\">09:00-17:45 </span>\n" +
                "    </div>\n" +
                "</div>");

        assertTrue(matcher.find());
        assertEquals("08:00-23:45", matcher.group(0));

        assertTrue(matcher.find());
        assertEquals("08:00-19:45", matcher.group(0));

        assertTrue(matcher.find());
        assertEquals("chiuso", matcher.group(0));

        assertTrue(matcher.find());
        assertEquals("chiuso", matcher.group(0));

        assertTrue(matcher.find());
        assertEquals("09:00-17:45", matcher.group(0));


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