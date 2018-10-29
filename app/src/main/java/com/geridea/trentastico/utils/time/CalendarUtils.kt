package com.geridea.trentastico.utils.time

import com.geridea.trentastico.Config
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE
import java.text.SimpleDateFormat
import java.util.*

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
object CalendarUtils {

    private val formatTimestamp  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ITALIAN)
    private val formatEEEEDDMMMM = SimpleDateFormat("EEEE dd MMMM", Locale.ITALIAN)
    private val formatHHMM       = SimpleDateFormat("HH:mm", Locale.ITALIAN)

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

    val clearCalendar: Calendar
        get() {
            val instance = Calendar.getInstance()
            instance.clear()
            instance.firstDayOfWeek = Calendar.MONDAY
            return instance
        }

    fun getCalendarWithMillis(millis: Long): Calendar {
        val instance = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"))
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

    fun addDays(millis: Long, delta: Int): Long {
        val cal = getCalendarWithMillis(millis)
        cal.add(Calendar.DATE, delta)
        return cal.timeInMillis
    }

    /**
     * Checks if two times are on the same day.
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    fun isSameDay(dayOne: Calendar, dayTwo: Calendar): Boolean = dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR) && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR)

    /**
     * @return a [Pair] containing the milliseconds of when the day starts and ends
     */
    fun getTodaysStartAndEndMs(): Pair<Long, Long> {
        val start = debuggableToday
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.HOUR_OF_DAY, 0)

        val end = debuggableToday
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.HOUR_OF_DAY, 23)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    /**
     * @return the formatted date, in a format such "Giovedì 1 Gen 2017, dalle 00:00 alle 01:00"
     */
    fun formatRangeComplete(startsAt: Long, endsAt: Long): CharSequence? =
            (formatEEEEDDMMMM(startsAt)+", dalle "+ formatHHMM(startsAt)+" alle "+ formatHHMM(endsAt)).capitalize()


    /**
     * Returns a calendar instance at the start of this day
     * @return the calendar instance
     */
    fun today(): Calendar {
        val today = debuggableToday
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today
    }
}
