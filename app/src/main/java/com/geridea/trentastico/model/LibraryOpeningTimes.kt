package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 28/04/2017.
 */

import java.text.SimpleDateFormat
import java.util.*

class LibraryOpeningTimes {

    /**
     * The day this opening times are referring about. In yyyy-MM-dd format.
     */
    var day: String? = null

    var timesBuc: String? = null
    var timesCial: String? = null
    var timesMesiano: String? = null
    var timesPovo: String? = null
    var timesPsicologia: String? = null

    companion object {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

        /**
         * Formats the day in such a way toe make it suitable for the URL request and the storing in
         * cache database.
         * @param day the day to format
         * @return the formatted day, in yyyy-MM-dd format
         */
        fun formatDay(day: Calendar): String = dateFormat.format(day.time)
    }
}
