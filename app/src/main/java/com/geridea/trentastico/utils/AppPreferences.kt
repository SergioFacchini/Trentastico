package com.geridea.trentastico.utils

import android.content.Context
import android.content.SharedPreferences
import com.geridea.trentastico.Config
import com.geridea.trentastico.gui.views.CustomWeekView
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.*
import com.geridea.trentastico.services.ShownNotificationsTracker
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

object AppPreferences {

    private var appContext: Context? = null

    /**
     * Here we're caching the extra courses list. It's being deserialized very often and this
     * caching is made to prevent this.
     */
    var extraCourses = ExtraCoursesList()
        private set

    fun init(context: Context) {
        appContext = context

        extraCourses = readExtraCourses()
    }

    private fun get(): SharedPreferences = if (appContext == null) {
        throw RuntimeException(
                "Preferences should be initialized by calling Preferences.init(...) method")
    } else {
        appContext!!.getSharedPreferences("null", Context.MODE_PRIVATE)
    }

    /**
     * @return true if tis the first time the application is run
     */
    var isFirstRun: Boolean
        get() = get().getBoolean("IS_FIRST_RUN", true)
        set(isFirstRun) = putBoolean("IS_FIRST_RUN", isFirstRun)

    var studyCourse: StudyCourse
        get() = StudyCourse.fromStringJson(
            get().getString("STUDY_COURSE", "{}")
        )
        set(course) {
            putString("STUDY_COURSE", course.toJson().toString())
            BugLogger.setStudyCourse(course)
        }

    var lessonTypesIdsToHide: ArrayList<Long>
        get() {
            val lessonTypesIds = ArrayList<Long>()

            val filteredJSON = get().getString("FILTERED_TEACHINGS", "[]")

            try {
                val json = JSONArray(filteredJSON)
                (0 until json.length()).mapTo(lessonTypesIds, { json.getLong(it) })
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return lessonTypesIds
        }
        set(teachings) {
            val array = JSONArray()
            for (teachingId in teachings) {
                array.put(teachingId)
            }

            putString("FILTERED_TEACHINGS", array.toString())
        }

    fun hasLessonTypeWithIdHidden(id: Long): Boolean = lessonTypesIdsToHide.contains(id)

    fun removeAllHiddenCourses() {
        lessonTypesIdsToHide = ArrayList()
    }

    private fun putInt(key: String, num: Int) {
        val editor = get().edit()
        editor.putInt(key, num)
        editor.apply()
    }

    var calendarNumOfDaysToShow: Int
        get() = get().getInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", Config.CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW)
        set(numOfDays) = putInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", numOfDays)

    private fun setPartitioningsToHide(lessonTypeId: String, partitioningCases: ArrayList<PartitioningCase>) = try {
        //Building values array
        val jsonArrayCases = JSONArray()
        for (aCase in partitioningCases) {
            jsonArrayCases.put(aCase.case)
        }

        //Saving partitionings
        val partitioningJSON = partitioningsJSON
        partitioningJSON.put(lessonTypeId, jsonArrayCases)

        putString("PARTITIONINGS_TO_HIDE", partitioningJSON.toString())
    } catch (e: JSONException) {
        BugLogger.logBug("Saving partitionings to hide", e)
    }

    private val partitioningsJSON: JSONObject
        get() {
            try {
                return JSONObject(get().getString("PARTITIONINGS_TO_HIDE", "{}"))
            } catch (e: JSONException) {
                BugLogger.logBug("Getting partitionings from JSON", e)
                e.printStackTrace()

                throw RuntimeException("Error reading partitionings JSON.")
            }

        }

    fun getHiddenPartitionings(lessonTypeId: Long): ArrayList<String> {
        val partitionings = ArrayList<String>()

        try {
            val hiddenPartitioningsArray = partitioningsJSON.optJSONArray(lessonTypeId.toString())
            if (hiddenPartitioningsArray != null) {
                for (i in 0..hiddenPartitioningsArray.length() - 1) {
                    partitionings.add(hiddenPartitioningsArray.getString(i))
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return partitionings
    }

    fun updatePartitioningsToHide(lesson: LessonType) = setPartitioningsToHide(lesson.id, lesson.findPartitioningsToHide())

    fun removeAllHiddenPartitionings() = putString("PARTITIONINGS_TO_HIDE", "{}")

    private fun readExtraCourses(): ExtraCoursesList {
        val courses = ExtraCoursesList()

        return try {
            val jsonArray = JSONArray(get().getString("EXTRA_COURSES", "[]"))
            for (i in 0 until jsonArray.length()) {
                courses.add(ExtraCourse.fromJSON(jsonArray.getJSONObject(i)))
            }
            courses
        } catch (e: JSONException) {
            BugLogger.logBug("Parsing existing extra courses", e)
            e.printStackTrace()

            throw RuntimeException("Could not get extra courses from AppPreferences.")
        }

    }

    fun addExtraCourse(course: ExtraCourse) {
        extraCourses.add(course)

        saveExtraCourses()
    }

    private fun saveExtraCourses() {
        putString("EXTRA_COURSES", extraCourses.toJSON().toString())

        BugLogger.setExtraCourses(extraCourses)
    }

    private fun putString(key: String, value: String?) {
        val editor = get().edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun hasExtraCourseWithId(lessonTypeId: String): Boolean = extraCourses.hasCourseWithId(lessonTypeId)

    fun removeExtraCoursesOfCourse(studyCourse: StudyCourse) {
        extraCourses.removeHaving(studyCourse)
        saveExtraCourses()
    }

    fun getExtraCoursesOfCourse(studyCourse: StudyCourse): ArrayList<ExtraCourse>
            = extraCourses.getExtraCoursesOfCourse(studyCourse)

    fun removeExtraCourse(lessonTypeId: String) {
        extraCourses.removeHavingLessonType(lessonTypeId)
        saveExtraCourses()
    }

    val isStudyCourseSet: Boolean
        get() = get().getLong("STUDY_DEPARTMENT", 0) != 0L &&
                get().getLong("STUDY_COURSE", 0) != 0L &&
                get().getInt("STUDY_YEAR", 0) != 0

    private fun putLong(key: String, time: Long) {
        val editor = get().edit()
        editor.putLong(key, time)
        editor.apply()
    }

    var nextLessonsUpdateTime: Long
        get() = get().getLong("NEXT_LESSONS_UPDATE_TIME", 0)
        set(time) = putLong("NEXT_LESSONS_UPDATE_TIME", time)

    fun hadInternetInLastCheck(): Boolean = get().getBoolean("HAD_INTERNET_DURING_LAST_LESSON_UPDATE", true)

    fun hadInternetInLastCheck(had: Boolean) = putBoolean("HAD_INTERNET_DURING_LAST_LESSON_UPDATE", had)

    private fun putBoolean(key: String, bool: Boolean) {
        val editor = get().edit()
        editor.putBoolean(key, bool)
        editor.apply()
    }

    var isSearchForLessonChangesEnabled: Boolean
        get() = get().getBoolean("SEARCH_LESSON_CHANGES", true)
        set(enabled) = putBoolean("SEARCH_LESSON_CHANGES", enabled)

    var isNotificationForLessonChangesEnabled: Boolean
        get() = get().getBoolean("SHOW_NOTIFICATION_ON_LESSON_CHANGES", true)
        set(enabled) = putBoolean("SHOW_NOTIFICATION_ON_LESSON_CHANGES", enabled)

    fun areNextLessonNotificationsEnabled(): Boolean = get().getBoolean("NEXT_LESSON_NOTIFICATION_ENABLED", true)

    fun setNextLessonNotificationsEnabled(enabled: Boolean) = putBoolean("NEXT_LESSON_NOTIFICATION_ENABLED", enabled)

    fun areNextLessonNotificationsFixed(): Boolean = get().getBoolean("NEXT_LESSON_NOTIFICATION_FIXED", false)

    fun setNextLessonNotificationsFixed(enabled: Boolean) = putBoolean("NEXT_LESSON_NOTIFICATION_FIXED", enabled)

    val androidId: String
        get() {
            var androidId = get().getString("ANDROID_ID", "")
            if (androidId!!.isEmpty()) {
                androidId = UUID.randomUUID().toString()
                putString("ANDROID_ID", androidId)
            }

            return androidId
        }

    var notificationTracker: ShownNotificationsTracker
        get() = ShownNotificationsTracker.fromJson(get().getString("NEXT_LESSON_NOTIFICATION_TRACKER", "{}"))
        set(tracker) = putString("NEXT_LESSON_NOTIFICATION_TRACKER", tracker.toJson().toString())

    var calendarFontSize: Int
        get() = get().getInt("CALENDAR_FONT_SIZE", CustomWeekView.DEFAULT_EVENT_FONT_SIZE)
        set(sizeInSp) = putInt("CALENDAR_FONT_SIZE", sizeInSp)

}