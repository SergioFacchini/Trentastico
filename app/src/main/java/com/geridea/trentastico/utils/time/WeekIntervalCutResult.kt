package com.geridea.trentastico.utils.time


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

class WeekIntervalCutResult @JvmOverloads constructor(
        /**
         * @return the interval that has been cut; technically the intersection between the original
         * interval and the cut one.
         */
        val cutInterval: WeekInterval, val firstRemaining: WeekInterval? = null, val secondRemaining: WeekInterval? = null) {

    /**
     * @return false if the the entire interval has been cut (the weeks of the cut interval were a
     * subset of the weeks of the cutter), true otherwise.
     */
    fun hasAnyRemainingResult(): Boolean {
        return firstRemaining != null || secondRemaining != null
    }

    fun hasOnlyOneResult(): Boolean {
        return firstRemaining != null && secondRemaining == null
    }
}
