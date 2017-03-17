package com.geridea.trentastico.utils;


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

import java.util.Collection;
import java.util.Iterator;

public class StringUtils {

    public static String implode(Collection<String> strings, String glue) {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iterator = strings.iterator();
        while (iterator.hasNext()){
            String stringToGlue = iterator.next();
            builder.append(stringToGlue);

            if (iterator.hasNext()) {
                builder.append(glue);
            }
        }

        return builder.toString();
    }
}
