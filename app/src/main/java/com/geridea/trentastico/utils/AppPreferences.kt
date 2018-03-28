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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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


    var nextLessonsUpdateTime: Long by LongPreferences("NEXT_LESSONS_UPDATE_TIME", 0)

    /**
     * @return true if it is the first time the application is run
     */
    var isFirstRun: Boolean                            by BooleanPreferences("IS_FIRST_RUN", true)
    var wasAppInBetaMessageShown: Boolean              by BooleanPreferences("APP_IS_IN_BETA_MESSAGE_SHOWN", false)
    var wasLastTimesCheckSuccessful: Boolean           by BooleanPreferences("WAS_LAST_TIMES_CHECK_SUCCESSFUL", true)
    var isSearchForLessonChangesEnabled: Boolean       by BooleanPreferences("SEARCH_LESSON_CHANGES", true)
    var isNotificationForLessonChangesEnabled: Boolean by BooleanPreferences("SHOW_NOTIFICATION_ON_LESSON_CHANGES", true)
    var nextLessonNotificationsEnabled: Boolean        by BooleanPreferences("NEXT_LESSON_NOTIFICATION_ENABLED", true)
    var nextLessonNotificationsFixed: Boolean          by BooleanPreferences("NEXT_LESSON_NOTIFICATION_FIXED", false)

    /**
     * True whenever the user must not be notified of the next lessons update.
     */
    var debugSkipNextLessonChangedNotification: Boolean by BooleanPreferences("SKIP_NEXT_LESSON_CHANGED_NOTIFICATION", false)

    var debugIsInDebugMode: Boolean by BooleanPreferences("IS_DEBUG_MODE", false)

    var calendarFontSize: Int        by IntPreferences("CALENDAR_FONT_SIZE", CustomWeekView.DEFAULT_EVENT_FONT_SIZE)
    var calendarNumOfDaysToShow: Int by IntPreferences("CALENDAR_NUM_OF_DAYS_TO_SHOW", Config.CALENDAR_DEFAULT_NUM_OF_DAYS_TO_SHOW)

    /**
     * The version that this app had when it was executed last time.
     */
    var lastVersionExecuted:Int by IntPreferences("LAST_VERSION_EXECUTED", 0)

    /**
     * The last zoom level that the user has pinched
     */
    var lastZoom:Int by IntPreferences("CALENDAR_ZOOM", 0)

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


    /////////////
    // UTILS
    /////////////

    private fun putBoolean(key: String, bool: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, bool)
        editor.apply()
    }

    private fun putInt(key: String, num: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, num)
        editor.apply()
    }

    private fun putString(key: String, value: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun putLong(key: String, time: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, time)
        editor.apply()
    }

    //////////////
    // PROPERTIES
    //////////////

    class BooleanPreferences(val key: String, val default: Boolean): ReadWriteProperty<AppPreferences, Boolean> {

        override fun getValue(thisRef: AppPreferences, property: KProperty<*>): Boolean =
                thisRef.sharedPreferences.getBoolean(key, default)

        override fun setValue(thisRef: AppPreferences, property: KProperty<*>, value: Boolean) {
            thisRef.putBoolean(key, value)
        }
    }

    class IntPreferences(private val key: String, private val default: Int) : ReadWriteProperty<AppPreferences, Int> {
        override fun getValue(thisRef: AppPreferences, property: KProperty<*>): Int =
                thisRef.sharedPreferences.getInt(key, default)

        override fun setValue(thisRef: AppPreferences, property: KProperty<*>, value: Int) {
            thisRef.putInt(key, value)
        }

    }

    class LongPreferences(private val key: String, private val default: Long) : ReadWriteProperty<AppPreferences, Long> {
        override fun getValue(thisRef: AppPreferences, property: KProperty<*>): Long =
                thisRef.sharedPreferences.getLong(key, default)

        override fun setValue(thisRef: AppPreferences, property: KProperty<*>, value: Long) {
            thisRef.putLong(key, value)
        }

    }


}

