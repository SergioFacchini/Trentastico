package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

import java.util.*
import java.util.regex.Pattern

object StringUtils {

    fun implode(items: Iterable<*>, glue: String): String {
        val builder = StringBuilder()

        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val stringToGlue = iterator.next().toString()
            builder.append(stringToGlue)

            if (iterator.hasNext()) {
                builder.append(glue)
            }
        }

        return builder.toString()
    }

    fun <T> implode(items: Array<T>, glue: String): String = implode(Arrays.asList(*items), glue)

    fun findMatchingStringIfAny(name: String, regex: String): String? {
        val matcher = Pattern.compile(regex).matcher(name)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * @return true if the string matched the given regular expression. Note that despite
     * [String.matches] this method does not require that the whole string matches
     * the expression.
     */
    fun containsMatchingRegex(regex: String, stringToMatch: String): Boolean = Pattern.compile(regex).matcher(stringToMatch).find()

    fun positionFormat(format: String, vararg items: Any): String {
        var format = format
        for (i in items.indices) {
            format = format.replace("{$i}", items[i].toString())
        }

        return format
    }
}
