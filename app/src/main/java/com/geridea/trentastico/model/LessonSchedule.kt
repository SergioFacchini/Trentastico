package com.geridea.trentastico.model

import com.geridea.trentastico.network.request.LessonsDiffResult
import com.geridea.trentastico.utils.NumbersUtils
import com.geridea.trentastico.utils.time.CalendarUtils.debuggableToday
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


data class LessonSchedule(
        /**
         * @return the unique identifier of the lesson
         */
        val id: String,
        val room: String,
        val teachersNames: String,
        val subject: String,
        /**
         * The name of the partitioning. Is null when there are no partitionings
         */
        val partitioningName: String?,
        val startsAt: Long,
        val endsAt: Long,
        val color: Int,
        val lessonTypeId: String) : Serializable {

    /**
     * Returns true if both the lessons probably refer to the exactly same lesson
     */
    fun isMeaningfullyEqualTo(that: LessonSchedule): Boolean =
               id               == that.id
            && startsAt         == that.startsAt
            && endsAt           == that.endsAt
            && room             == that.room
            && partitioningName == that.partitioningName
            && teachersNames    == that.teachersNames
            && subject          == that.subject

    val startCal: Calendar
        get() {
            val calendar = debuggableToday
            calendar.timeInMillis = startsAt
            return calendar
        }

    val endCal: Calendar
        get() {
            val calendar = debuggableToday
            calendar.timeInMillis = endsAt
            return calendar
        }

    val synopsis: String
        get() {
            val room = room

            val hhmm = SimpleDateFormat("HH:mm")
            val startTime = hhmm.format(startCal.time)
            val endTime = hhmm.format(endCal.time)

            return if (room.isEmpty()) {
                String.format("%s-%s", startTime, endTime)
            } else {
                String.format("%s-%s | %s", startTime, endTime, room)
            }
        }

    val eventDescription: String
        get() {
            return "${subject.toUpperCase()}\n" +
                    "$teachersNames\n" +
                    (if(partitioningName != null) "$partitioningName\n" else "") +
                    room
        }

    val durationInMinutes: Int
        get() = TimeUnit.MILLISECONDS.toMinutes(endsAt - startsAt).toInt()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as LessonSchedule?

        return id == that!!.id
                && startsAt == that.startsAt
                && endsAt == that.endsAt
                && color == that.color
                && lessonTypeId == that.lessonTypeId
                && room == that.room
                && subject == that.subject
    }

    fun startsBefore(currentMillis: Long): Boolean = startsAt < currentMillis

    fun startsAfter(currentMillis: Long): Boolean = startsAt > currentMillis

    fun isHeldInMilliseconds(ms: Long): Boolean = startsAt >= ms && ms <= endsAt

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + room.hashCode()
        result = 31 * result + teachersNames.hashCode()
        result = 31 * result + subject.hashCode()
        result = 31 * result + startsAt.hashCode()
        result = 31 * result + endsAt.hashCode()
        result = 31 * result + color
        result = 31 * result + lessonTypeId.hashCode()
        return result
    }


    companion object {

        fun diffLessons(
                cachedLessons: ArrayList<LessonSchedule>,
                fetchedLessons: ArrayList<LessonSchedule>): LessonsDiffResult {

            val diffResult = LessonsDiffResult()

            sortByStartDateOrId(fetchedLessons)
            sortByStartDateOrId(cachedLessons)

            val fetchedNotCachedLesson = ArrayList(fetchedLessons)
            for (cached in cachedLessons) {

                var cacheLessonFound = false
                for (fetched in fetchedLessons) {
                    if (cached.id == fetched.id) {
                        //We found the lesson
                        fetchedNotCachedLesson.remove(fetched)

                        if (!cached.isMeaningfullyEqualTo(fetched)) {
                            diffResult.addChangedLesson(cached, fetched)
                        }

                        cacheLessonFound = true
                        break
                    }
                }

                if (!cacheLessonFound) {
                    diffResult.addRemovedLesson(cached)
                }
            }

            for (lessonNotInCache in fetchedNotCachedLesson) {
                diffResult.addAddedLesson(lessonNotInCache)
            }

            return diffResult
        }

        private fun sortByStartDateOrId(lessons: ArrayList<LessonSchedule>) = Collections.sort(lessons) { a, b ->
            var compare = NumbersUtils.compare(a.startsAt, b.startsAt)
            if (compare == 0) {
                compare = a.id.compareTo(b.id)
            }

            compare
        }
    }

}
