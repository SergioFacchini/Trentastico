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
import org.json.JSONObject
import java.util.*

object AppPreferences {


    private lateinit var sharedPreferences: SharedPreferences

    /**
     * Here we're caching the extra courses list. It's being deserialized very often and this
     * caching is made to prevent this.
     */
    var extraCourses = ExtraCoursesList()
        private set

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("null", Context.MODE_PRIVATE)

        extraCourses = readExtraCourses()
    }

    /**
     * @return true if tis the first time the application is run
     */
    var isFirstRun: Boolean
        get() = sharedPreferences.getBoolean("IS_FIRST_RUN", true)
        set(isFirstRun) = putBoolean("IS_FIRST_RUN", isFirstRun)

    var studyCourse: StudyCourse
        get() {
            val studyCourseJson = getStudyCourseJSON()
            if (studyCourseJson != null) {
                return StudyCourse(studyCourseJson)
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

            val filteredJSON = sharedPreferences.getString("FILTERED_TEACHINGS", "[]")

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
        val editor = sharedPreferences.edit()
        editor.putInt(key, num)
        editor.apply()
    }

    var calendarNumOfDaysToShow: Int
        get() = sharedPreferences.getInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", Config.CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW)
        set(numOfDays) = putInt("CALENDAR_NUM_OF_DAYS_TO_SHOW", numOfDays)

    private fun readExtraCourses(): ExtraCoursesList {
        val courses = ExtraCoursesList()

        return try {
            val jsonArray = JSONArray(sharedPreferences.getString("EXTRA_COURSES", "[]"))
            (0 until jsonArray.length()).mapTo(courses) { ExtraCourse.fromJSON(jsonArray.getJSONObject(it)) }
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
        val editor = sharedPreferences.edit()
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

    private fun getStudyCourseJSON():JSONObject? {
        val jsonString:String? = sharedPreferences.getString("STUDY_COURSE", null)
        return if(jsonString == null) null else JSONObject(jsonString)
    }

    private fun putLong(key: String, time: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, time)
        editor.apply()
    }

    var nextLessonsUpdateTime: Long
        get() = sharedPreferences.getLong("NEXT_LESSONS_UPDATE_TIME", 0)
        set(time) = putLong("NEXT_LESSONS_UPDATE_TIME", time)

    var wasLastTimesCheckSuccessful: Boolean
      get() = sharedPreferences.getBoolean("WAS_LAST_TIMES_CHECK_SUCCESSFUL", true)
      set(value) = putBoolean("WAS_LAST_TIMES_CHECK_SUCCESSFUL", value)

    private fun putBoolean(key: String, bool: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, bool)
        editor.apply()
    }

    var isSearchForLessonChangesEnabled: Boolean
        get() = sharedPreferences.getBoolean("SEARCH_LESSON_CHANGES", true)
        set(enabled) = putBoolean("SEARCH_LESSON_CHANGES", enabled)

    var isNotificationForLessonChangesEnabled: Boolean
        get() = sharedPreferences.getBoolean("SHOW_NOTIFICATION_ON_LESSON_CHANGES", true)
        set(enabled) = putBoolean("SHOW_NOTIFICATION_ON_LESSON_CHANGES", enabled)

    fun areNextLessonNotificationsEnabled(): Boolean = sharedPreferences.getBoolean("NEXT_LESSON_NOTIFICATION_ENABLED", true)

    fun areNextLessonNotificationsFixed(): Boolean = sharedPreferences.getBoolean("NEXT_LESSON_NOTIFICATION_FIXED", false)

    val androidId: String
        get() {
            var androidId = sharedPreferences.getString("ANDROID_ID", "")
            if (androidId!!.isEmpty()) {
                androidId = UUID.randomUUID().toString()
                putString("ANDROID_ID", androidId)
            }

            return androidId
        }

    var notificationTracker: ShownNotificationsTracker
        get() = ShownNotificationsTracker.fromJson(sharedPreferences.getString("NEXT_LESSON_NOTIFICATION_TRACKER", "{}"))
        set(tracker) = putString("NEXT_LESSON_NOTIFICATION_TRACKER", tracker.toJson().toString())

    var calendarFontSize: Int
        get() = sharedPreferences.getInt("CALENDAR_FONT_SIZE", CustomWeekView.DEFAULT_EVENT_FONT_SIZE)
        set(sizeInSp) = putInt("CALENDAR_FONT_SIZE", sizeInSp)

}