package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 26/04/2017.
 */

object CollectionUtils {

    fun <T : Enum<T>> isOneOf(needle: Enum<T>, vararg haystack: Enum<T>): Boolean =
            haystack.any { needle === it }

}

/**
 * If not empty returns itself, if empty returns the alternative
 */
fun <E> List<E>.orIfEmpty(alternative: List<E>): List<E> =
        if (isEmpty()) alternative else this

/**
 * @return true if all elements of the array contain exactly the same elements
 * (evaluated by [Any.equals]).
 */
fun <T, E> Collection<E>.allSame(mapper: (E) -> T): Boolean {
    return if (isEmpty()) true
           else {
               val firstElement = mapper(first())
               all { mapper(it) == (firstElement) }
           }
}