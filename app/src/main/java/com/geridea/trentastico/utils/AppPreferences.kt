package com.geridea.trentastico.utils

import android.content.Context
import android.content.SharedPreferences
import com.geridea.trentastico.Config
import com.geridea.trentastico.gui.views.CustomWeekView
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.model.ExtraCoursesList
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.services.ShownNotificationsTracker
import org.json.JSONArray
import org.json.JSONException
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
        get() {
            val studyCourseJson = getStudyCourseJSON()
            if (studyCourseJson != null) {
                return StudyCourse.fromStringJson(studyCourseJson)
            } else {
                throw RuntimeException("Cannot ask for study course until it's initialized!")
            }
        }
        set(course) {
            putString("STUDY_COURSE", course.toJson().toString())
            BugLogger.setStudyCourse(course)
        }

    var lessonTypesToHideIds: ArrayList<String>
        get() {
            val lessonTypesIds = ArrayList<String>()

            val filteredJSON = get().getString("FILTERED_TEACHINGS", "[]")

            try {
                val json = JSONArray(filteredJSON)
                (0 until json.length()).mapTo(lessonTypesIds, { json.getString(it) })
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

    /**
     * @return true if the passes lesson type is among the ones that were hidden (set not visible) by the user,
     * false otherwise
     */
    fun isLessonTypeToHide(lessonTypeId: String): Boolean = lessonTypesToHideIds.contains(lessonTypeId)

    fun removeAllHiddenCourses() {
        lessonTypesToHideIds = ArrayList()
    }

    private fun putInt(key: String, num: Int) {
        val editor = get().edit()
        editor.putInt(key, num)
        editor.apply()
    }

    var calendarNumOfDaysToShow: Int
        get() = get().getInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", Config.CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW)
        set(numOfDays) = putInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", numOfDays)

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
        get() = getStudyCourseJSON() != null

    private fun getStudyCourseJSON():String? = get().getString("STUDY_COURSE", null)

    private fun putLong(key: String, time: Long) {
        val editor = get().edit()
        editor.putLong(key, time)
        editor.apply()
    }

    var nextLessonsUpdateTime: Long
        get() = get().getLong("NEXT_LESSONS_UPDATE_TIME", 0)
        set(time) = putLong("NEXT_LESSONS_UPDATE_TIME", time)

    var wasLastTimesCheckSuccessful: Boolean
      get() = get().getBoolean("WAS_LAST_TIMES_CHECK_SUCCESSFUL", true)
      set(value) = putBoolean("WAS_LAST_TIMES_CHECK_SUCCESSFUL", value)

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