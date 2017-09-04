package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

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

    /**
     * @return true if the string matched the given regular expression. Note that despite
     * String.matches this method does not require that the whole string matches
     * the expression.
     */
    fun containsMatchingRegex(regex: String, stringToMatch: String): Boolean = Pattern.compile(regex).matcher(stringToMatch).find()

    fun positionFormat(originalFormat: String, vararg items: Any): String {
        var format = originalFormat

        items.indices.forEach { i -> format = format.replace("{$i}", items[i].toString()) }

        return format
    }
}

fun String.orIfBlank(alternative: String): String =
        if (this.isBlank()) alternative else this