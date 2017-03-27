package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static String implode(Collection strings, String glue) {
        StringBuilder builder = new StringBuilder();

        Iterator iterator = strings.iterator();
        while (iterator.hasNext()){
            String stringToGlue = iterator.next().toString();
            builder.append(stringToGlue);

            if (iterator.hasNext()) {
                builder.append(glue);
            }
        }

        return builder.toString();
    }

    @Nullable
    public static String findMatchingStringIfAny(String name, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(name);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * @return true if the string matched the given regular expression. Note that despite
     * {@link String#matches(String)} this method does not require that the whole string matches
     * the expression.
     */
    public static boolean containsMatchingRegex(String regex, String stringToMatch) {
        return Pattern.compile(regex).matcher(stringToMatch).find();
    }
}
