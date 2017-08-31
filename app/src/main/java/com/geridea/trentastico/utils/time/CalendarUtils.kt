package com.geridea.trentastico.utils.time

import com.geridea.trentastico.Config
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE
import java.text.SimpleDateFormat
import java.util.*

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
object CalendarUtils {

    val SECONDS_MS = 1000
    val MINUTE_MS = 60 * SECONDS_MS
    val HOUR_MS = 60 * MINUTE_MS
    val DAY_MS = 24 * HOUR_MS

    private val formatDDMMYY = SimpleDateFormat("dd MM yyyy", Locale.ITALIAN)
    private val formatTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ITALIAN)
    private val formatEEEEDDMMMM = SimpleDateFormat("EEEE dd MMMM", Locale.ITALIAN)
    private val formatHHMM = SimpleDateFormat("HH:mm", Locale.ITALIAN)

    /**
     * @return the first day of the week containing the specified date, at 00:00:00.
     */
    @JvmOverloads
    fun calculateFirstDayOfWeek(date: Calendar = debuggableToday): Calendar {
        val firstDayOfWeek = date.clone() as Calendar
        firstDayOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        firstDayOfWeek.clear(Calendar.MINUTE)
        firstDayOfWeek.clear(Calendar.SECOND)
        firstDayOfWeek.clear(Calendar.MILLISECOND)
        firstDayOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return firstDayOfWeek
    }

    fun formatDDMMYY(calendar: Calendar): String = formatDDMMYY.format(calendar.time)

    val purgedCalendarInstance: Calendar
        get() {
            val instance = Calendar.getInstance()
            instance.clear()

            return instance
        }

    val clearCalendar: Calendar
        get() {
            val instance = Calendar.getInstance()
            instance.clear()
            instance.firstDayOfWeek = Calendar.MONDAY
            return instance
        }

    fun getCalendarInitializedAs(calendar: Calendar): Calendar {
        val instance = Calendar.getInstance()
        instance.clear()
        instance.firstDayOfWeek = Calendar.MONDAY
        instance.time = calendar.time
        return instance
    }

    fun getCalendarWithMillis(millis: Long): Calendar {
        val instance = Calendar.getInstance()
        instance.clear()
        instance.firstDayOfWeek = Calendar.MONDAY
        instance.timeInMillis = millis
        return instance
    }

    /**
     * @return yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    fun formatTimestamp(millis: Long): String = formatTimestamp.format(Date(millis))

    fun formatEEEEDDMMMM(millis: Long): String = formatEEEEDDMMMM.format(Date(millis))

    fun formatHHMM(millis: Long): String = formatHHMM.format(Date(millis))

    val debuggableToday: Calendar
        get() {
            val aDay = Calendar.getInstance()
            if (IS_IN_DEBUG_MODE && Config.DEBUG_FORCE_ANOTHER_DATE) {
                aDay.timeInMillis = Config.DATE_TO_FORCE
            }

            return aDay
        }

    val debuggableMillis: Long
        get() = if (IS_IN_DEBUG_MODE && Config.DEBUG_FORCE_ANOTHER_DATE) Config.DATE_TO_FORCE else System.currentTimeMillis()

    fun getMillisWithMinutesDelta(delta: Int): Long {
        val now = debuggableToday
        now.add(Calendar.MINUTE, delta)
        return now.timeInMillis
    }

    fun addMinutes(millis: Long, delta: Int): Long {
        val cal = getCalendarWithMillis(millis)
        cal.add(Calendar.MINUTE, delta)
        return cal.timeInMillis
    }

    /**
     * Checks if two times are on the same day.
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    fun isSameDay(dayOne: Calendar, dayTwo: Calendar): Boolean = dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR) && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR)
}
/**
 * @return the first day of the current week, at 00:00:00.
 */
