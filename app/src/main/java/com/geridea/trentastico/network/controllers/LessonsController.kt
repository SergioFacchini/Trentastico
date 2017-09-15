package com.geridea.trentastico.network.controllers

import com.geridea.trentastico.Config
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.database.TodaysLessonsListener
import com.geridea.trentastico.gui.views.requestloader.CacheLessonsLoadingMessage
import com.geridea.trentastico.gui.views.requestloader.CacheLoadingFinishedMessage
import com.geridea.trentastico.gui.views.requestloader.ExtraLessonsLoadingMessage
import com.geridea.trentastico.gui.views.requestloader.StandardLessonsLoadingMessage
import com.geridea.trentastico.model.*
import com.geridea.trentastico.network.controllers.listener.CoursesLoadingListener
import com.geridea.trentastico.network.controllers.listener.DiffLessonsListener
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.network.request.ServerResponseParsingException
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ColorDispenser
import com.geridea.trentastico.utils.allSame
import com.geridea.trentastico.utils.orIfBlank
import okhttp3.FormBody
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


/*
 * Created with ♥ by Slava on 18/08/2017.
 */

class LessonsController(private val sender: RequestSender, private val cacher: Cacher) {

    /**
     * Loads from the website all the study courses and returns them through the listener
     */
    fun loadStudyCourses(listener: CoursesLoadingListener) =
            sender.processRequest(LoadStudyCoursesRequest(listener))

    fun loadStandardLessons(listener: LessonsLoadingListener, studyCourse: StudyCourse) {
        val operation = CacheLessonsLoadingMessage()
        listener.onLoadingMessageDispatched(operation)

        cacher.getStandardLessonsAndTypes({ cachedLessons, cachedTypes ->
            listener.onLoadingMessageDispatched(CacheLoadingFinishedMessage(operation.messageId))

            if (cachedLessons.isNotEmpty() && cachedTypes.isNotEmpty()) {
                listener.onLessonsLoaded(cachedLessons, cachedTypes, -1)
            } else {
                //No cache available, we have to download the data
                sender.processRequest(LoadStandardLessonsRequest(studyCourse, listener, cacher))
            }
        })
    }

    /**
     * Loads standard and extra lessons that are held today
     */
    fun loadTodaysCachedLessons(todaysLessonsListener: TodaysLessonsListener) =
            cacher.loadTodaysLessons(todaysLessonsListener)

    fun loadCachedLessonTypes(callback: (List<LessonType>) -> Unit) =
            cacher.fetchLessonTypes(callback)

    fun loadLessonTypesOfStudyCourse(studyCourse: StudyCourse, listener: ListLessonsListener) =
            sender.processRequest(LessonTypesOfStudyCourseRequest(studyCourse, listener))

    fun loadExtraCourseLessons(listener: LessonsLoadingListener, extraCourse: ExtraCourse) {
        val operation = CacheLessonsLoadingMessage()
        listener.onLoadingMessageDispatched(operation)

        cacher.fetchExtraScheduledLessons(extraCourse.lessonTypeId) { cachedLessons ->
            listener.onLoadingMessageDispatched(CacheLoadingFinishedMessage(operation.messageId))

            if (cachedLessons.isNotEmpty()) {
                val lessonType = LessonType(extraCourse, !AppPreferences.isLessonTypeToHide(extraCourse.lessonTypeId))
                listener.onLessonsLoaded(cachedLessons, arrayListOf(lessonType), -1)
            } else {
                //No cache
                sender.processRequest(LessonsOfExtraCourseRequest(extraCourse, listener, cacher))
            }

        }
    }

    fun diffStudyCourseLessonsWithCachedOnes(
            lastValidTimestamp: Long? = null,
            listener: DiffLessonsListener) {
        cacher.getStandardLessonsAndTypes({ cachedLessons, cachedTypes ->
            //Is there any cached lesson?
            if (cachedLessons.isEmpty() || cachedTypes.isEmpty()) {
                listener.onNoCachedLessons()
                return@getStandardLessonsAndTypes
            }

            sender.processRequest(DiffStudyCourseRequest(
                    AppPreferences.studyCourse, listener, cachedLessons, cacher, lastValidTimestamp
            ))
        })
    }

    fun diffExtraCourseLessonsWithCachedOnes(
            course: ExtraCourse,
            lastValidTimestamp: Long? = null,
            listener: DiffLessonsListener) {
        cacher.fetchExtraScheduledLessons(course.lessonTypeId) { cachedLessons ->
            if (cachedLessons.isEmpty()) {
                listener.onNoCachedLessons()
                return@fetchExtraScheduledLessons
            } else {
                sender.processRequest(DiffExtraCourseRequest(
                        course, listener, cachedLessons, cacher, lastValidTimestamp
                ))
            }
        }
    }

    fun purgeStudyCourseCache() {
        cacher.purgeStudyCourseCache()
    }

    fun removeExtraCoursesWithLessonType(lessonTypeId: String) {
        cacher.removeExtraCoursesWithLessonType(lessonTypeId)
    }

    fun obliterateCache() {
        cacher.obliterateCache()
    }

}


internal abstract class BasicRequest : IRequest {

    internal val operationId = nextAvailableId

    companion object {
        private var nextAvailableId = 1
            get() = field++
    }

}

//////////////////
/// Study courses
/////////////////
/**
 * Requests that load all the available courses
 * @param removeAllCoursesOption remove from the courses list the "all-courses" option
 */
internal class LoadStudyCoursesRequest(
        val listener: CoursesLoadingListener,
        private val removeAllCoursesOption: Boolean = true) : IRequest {

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onLoadingError()
    }

    private val jsonRegex = Pattern.compile("var elenco_corsi = ([^;]+?);\\svar")

    override val url: String
        get() = "https://easyroom.unitn.it/Orario/combo_call.php"

    override val formToSend: FormBody?
        get() = null

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onParsingError(e)
    }

    override fun manageResponse(responseToManage: String, sender: RequestSender) {
        /* The response is a huge javascript file having about ten variables initialized with
           json data. We want the json associated to the "elenco_corsi" variable */
        val matcher = jsonRegex.matcher(responseToManage)
        if (matcher.find()) {
            val foundJson = matcher.group(1)

            //Now we have the json to parse. It's structure is the following:
            /*
           [
             {
                  "label":"2017\/2018",
                  "valore":"2017",
                  "elenco":[
                     {
                        "label":"Amministrazione aziendale e diritto (triennale)",
                        "valore":"0115G",
                        "elenco_anni":[
                           {
                              "label":"1 anno - Generale",
                              "valore":"P0001|1"
                           },
                           {
                              "label":"2 anno - Generale",
                              "valore":"P0001|2"
                           },
                           {
                              "label":"3 anno - Generale",
                              "valore":"P0001|3"
                           },
                           {
                              "label":"Generale - Tutti gli anni",
                              "valore":"P0001|0"
                           }
                        ]
                     }, {
                        "label":"Economia e legislazione d'impresa (magistrale)",
                        "valore":"0125H",
                        "elenco_anni":[
                           {
                              "label":"1 anno - Standard",
                              "valore":"P0001|1"
                           },
                           {
                              "label":"2 anno - Standard",
                              "valore":"P0001|2"
                           },
                           {
                              "label":"Standard - Tutti gli anni",
                              "valore":"P0001|0"
                           }
                        ]
                     }
                 ]
             }
           ]
           */
            val currentYearJson = findCurrentYearJson(JSONArray(foundJson))
            val currentYearCourses = currentYearJson.getJSONArray("elenco")

            //The currentYearJson contains the information about the courses of the current year
            //we have to parse it

            listener.onCoursesFetched(List(currentYearCourses.length()) {
                val courseJson = currentYearCourses[it] as JSONObject
                var studyYears = parseStudyYears(courseJson.getJSONArray("elenco_anni"))

                if (removeAllCoursesOption) studyYears = studyYears.filterNot { it.name.contains("Tutti gli anni") }

                Course(
                        id = courseJson.getString("valore"),
                        name = courseJson.getString("label"),
                        studyYears = studyYears
                )
            })

        } else {
            throw ServerResponseParsingException("The data javascript is not properly formatted!")
        }
    }

    private fun parseStudyYears(yearsJson: JSONArray): List<StudyYear> {
        return List(yearsJson.length(), {
            val yearJson = yearsJson[it] as JSONObject

            StudyYear(
                    id = yearJson.getString("valore"),
                    name = yearJson.getString("label")
            )
        })
    }

    private fun findCurrentYearJson(yearsJsonArray: JSONArray): JSONObject {
        return (0 until yearsJsonArray.length())
                .map { yearsJsonArray[it] as JSONObject }
                .firstOrNull { it.getString("valore") == Config.CURRENT_STUDY_YEAR }
                ?: throw ServerResponseParsingException("Cannot find current year JSON!")
    }

    override fun notifyOnBeforeSend() { ; }

}

/**
 * Basic request that downloads a list of lesson schedules and lesson types.
 * To be notified of the results of the request override [BasicLessonRequest.onTeachingsAndLessonsLoaded]
 */
internal abstract class BasicLessonRequest(val studyCourse: StudyCourse) : BasicRequest() {

    /**
     * The colors are assigned sequentially: that means that the first lesson type picks the first
     * color, the second picks the seconds.. and so on.
     */
    override fun manageResponse(responseToManage: String, sender: RequestSender) {
        /* Sample response:
        {
          "0": {
            "display": [ "nome_insegnamento", "docente", "aula", "orario", "tipo"],
            "codice_insegnamento": "EC0119H_121393_121393\/2_Nessun partizionamento_ERZEG",
            "nome_insegnamento": "International accounting and finance - nessun partizionamento",
            "codice_docente": "076763, 004407",
            "docente": "Corvo Stefano, Erzegovesi Luca",
            "timestamp": 1505896200,
            "data": "20-09-2017",
            "anno": "2017",
            "codice_aula": "E0101\/A21",
            "aula": "Aula 3C [Dipartimento di Economia e Management]",
            "orario": "08:30 - 10:30",
            "tipo": "Lezione",
            "numero_giorno": "3",
            "nome_giorno": "mercoled\u00ec",
            "ora_inizio": "08:30",
            "ora_fine": "10:30"
          },
          "1": {
            "display": ["nome_insegnamento", "docente", "aula", "orario", "tipo"],
            "codice_insegnamento": "EC0119H_121393_121393\/2_Nessun partizionamento_ERZEG",
            "nome_insegnamento": "International accounting and finance - nessun partizionamento",
            "codice_docente": "076763, 004407",
            "docente": "Corvo Stefano, Erzegovesi Luca",
            "timestamp": 1505982600,
            "data": "21-09-2017",
            "anno": "2017",
            "codice_aula": "E0101\/A21",
            "aula": "Aula 3C [Dipartimento di Economia e Management]",
            "orario": "08:30 - 10:30",
            "tipo": "Lezione",
            "numero_giorno": "4",
            "nome_giorno": "gioved\u00ec",
            "ora_inizio": "08:30",
            "ora_fine": "10:30"
          },
          "now_timestamp": 1503181005,
          "tipo": "corso",
          "aa": "2017\/2018",
          "anno": "2017",
          "check": 1,
          "contains_data": 2,
          "day": "1",
          "colori": ["#FFFCB1", "#FFE6BB", "#B9F4FF", "#F3BAF5"]
        }
        */
        val json = JSONObject(responseToManage)
        val lessonTypes = parseLessonTypes(json)
        val lessons = parseLessons(json)

        //Fixes #100: If all lessons have the same partitioning, then there is no partitioning
        if(doAllLessonTypesHaveSamePartitioning(lessonTypes)) {
            lessonTypes.forEach { it.partitioningName = null }
            lessons    .forEach { it.partitioningName = null }
        }

        onTeachingsAndLessonsLoaded(lessonTypes, lessons)
    }

    private fun doAllLessonTypesHaveSamePartitioning(lessonTypes: List<LessonType>): Boolean =
            lessonTypes.allSame { it.partitioningName }

    /**
     * Hook function that notifies that the loading has been completed
     */
    abstract fun onTeachingsAndLessonsLoaded(
            lessonTypes: List<LessonType>,
            loadedLessons: List<LessonSchedule>)

    private fun parseLessonTypes(json: JSONObject): List<LessonType> {
        //To get a list of teaching we might do the followings:
        // * Make another request to obtain a list
        // * Scan all the items in search of the teachings
        //The second one looks easier to implement, so I'll follow that way
        val numLessons = json.getInt("contains_data")

        return (0 until numLessons)
                .map { json[it.toString()] as JSONObject }
                .distinctBy { it.getString("codice_insegnamento") }
                .map {
                    val lessonTypeId = it.getString("codice_insegnamento")
                    val teachingName = it.getString("nome_insegnamento")
                    val partitioningName = calculatePartitioningName(teachingName)
                    LessonType(
                            id                    = lessonTypeId,
                            name                  = calculateTeachingWithoutPartitioning(teachingName),
                            teachersNamesUnparsed = it.getString("docente"),
                            teachersCodesToParse  = it.getString("codice_docente"),
                            partitioningName      = partitioningName,
                            kindOfLesson          = it.getString("tipo"),
                            isVisible             = !AppPreferences.isLessonTypeToHide(lessonTypeId)
                    )
                }
                .sortedBy { it.name }
    }

    /**
     * @param json the json to parse
     */
    private fun parseLessons(json: JSONObject): List<LessonSchedule> {
        val lessonStartFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ITALIAN)

        val numLessons = json.getInt("contains_data")

        return (0 until numLessons).map {
            val lessonJson = json[it.toString()] as JSONObject

            //WARNING: The "timestamp" field in the json is SCREWED for some lessons. Yeeee!
            //We have to build the timing ourselves
            val lessonDateString = lessonJson.getString("data")+" "+lessonJson.getString("ora_inizio")
            val startTimestamp    = lessonStartFormat.parse(lessonDateString).time

            val startingMins = convertToMinutes(lessonJson.getString("ora_inizio"))
            val endingMins   = convertToMinutes(lessonJson.getString("ora_fine"))

            val teachingName = lessonJson.getString("nome_insegnamento")

            LessonSchedule(
                    id               = generateLessonScheduleId(lessonJson, startTimestamp),
                    lessonTypeId     = lessonJson.getString("codice_insegnamento"),
                    teachersNames    = lessonJson.getString("docente").orIfBlank(NO_TEACHER_ASSIGNED_DEFAULT_TEXT),
                    rooms            = calculateRooms(lessonJson.getString("aula")),
                    subject          = calculateTeachingWithoutPartitioning(teachingName),
                    partitioningName = calculatePartitioningName(teachingName),
                    startsAt         = startTimestamp,
                    endsAt           = calculateEndTimeOfLesson(startTimestamp, endingMins, startingMins)
            )
        }
    }

    private fun generateLessonScheduleId(lessonJson: JSONObject, startTimestamp: Long): String {
        //The lesson schedules do not have any identifier. We introduce it by merging the lesson
        //code with the lesson's timestamp and it's room
        val teachingId = lessonJson.getString("codice_insegnamento")
        val roomName   = lessonJson.getString("aula")

        return "$teachingId@$startTimestamp@$roomName"
    }

    private fun calculateRooms(string: String): List<Room> {
        //The rooms are in format "Room name[Department Name],Room name2[Department Name 2]"
        val pattern = Pattern.compile("^ ?(.+)\\[(.+)\\]")
        return string.split(",").map {
            val matcher = pattern.matcher(it)
            if (matcher.find()) {
                Room(room = matcher.group(1), department = matcher.group(2))
            } else {
                //Not all the rooms have the department specified
                Room(room = string, department = null)
            }
        }

    }

    private fun calculateTeachingWithoutPartitioning(teachingName: String): String =
            //The name of the partitioning is contained in the name of the course and is
            //preceded by a dash with spaces " - ". Some courses names however are malformed and there
            // are no dashes in their names. In this case we assume that there are no partitionings.
            when {
                teachingName.endsWith("_LEZ") -> teachingName.substring(0, teachingName.length-4)
                teachingName.endsWith("_LAB") -> teachingName.substring(0, teachingName.length-4)
                teachingName.contains("-")    -> teachingName.take(teachingName.lastIndexOf(" - "))
                else                          -> teachingName
            }

    private fun calculatePartitioningName(teachingName: String): String? =
            //The name of the partitioning is contained in the name of the course and is
            //preceded by a dash with spaces " - ". Some courses names however are malformed and there
            // are no dashes in their names. In this case we assume that there are no partitionings.
            when {
                teachingName.endsWith("_LAB") -> "LAB" //Fixes #108
                teachingName.endsWith("_LEZ") -> "LEZ" //Fixes #108
                teachingName.endsWith("uomo - macchina") -> null //Fixes #107
                !teachingName.contains("-")   -> null
                else -> {
                    val partitioningName = teachingName.takeLastWhile { it != '-' }.trim()
                    when {
                        partitioningName == "nessun partizionamento"          -> null
                        partitioningName.toLowerCase() in arrayOf("lm", "n0") -> null //Fixes #104
                        else                                                    -> partitioningName
                    }
                }
            }

    private fun calculateEndTimeOfLesson(
            startTimestamp: Long,
            endingMins: Int,
            startingMins: Int): Long {
        val endingCalendar = Calendar.getInstance()
        endingCalendar.timeInMillis = startTimestamp
        endingCalendar.add(Calendar.MINUTE, endingMins - startingMins)

        return endingCalendar.timeInMillis
    }

    /**
     * Converts a hours string like "8:30" to minutes (8*60 + 30)
     */
    private fun convertToMinutes(string: String): Int {
        val matcher = Pattern.compile("([0-9][0-9]?):([0-9][0-9])").matcher(string)
        if (matcher.find()) {
            val hours = matcher.group(1).toInt()
            val minutes = matcher.group(2).toInt()

            return hours * 60 + minutes
        } else {
            throw IllegalArgumentException("The minutes to convert must be in \"hh:mm\" format!")
        }
    }

    @Suppress("ConstantConditionIf")
    override val url: String
        get() = if (Config.LAUNCH_LESSONS_REQUESTS_TO_DEBUG_SERVER) Config.DEBUG_SERVER_URL
        else "https://easyroom.unitn.it/Orario/list_call.php"

    override val formToSend: FormBody?
        get() = FormBody.Builder()
                .add("form-type", "corso")
                .add("aa", Config.CURRENT_STUDY_YEAR)
                .add("anno", Config.CURRENT_STUDY_YEAR)
                .add("corso", studyCourse.courseId)
                .add("anno2", studyCourse.yearId)
                .build()

    override fun notifyOnBeforeSend() {; }
}

/**
 * Requests that load the lessons of the current StudyCourse
 * @param assignColors whenever the [ColorDispenser] should keep tracks of the colors of the fetched
 * lesson types
 */
internal class LoadStandardLessonsRequest(
        studyCourse: StudyCourse,
        listener: LessonsLoadingListener,
        private val cacher: Cacher,
        private val assignColors: Boolean = true
) : BasicLessonRequest(studyCourse) {

    private val weakListener: WeakReference<LessonsLoadingListener> = WeakReference(listener)

    override fun onTeachingsAndLessonsLoaded(
            lessonTypes: List<LessonType>,
            loadedLessons: List<LessonSchedule>) {

        if (assignColors) {
            lessonTypes.forEach { ColorDispenser.associateColorToTypeIfNeeded(it.id)  }
        }

        cacher.cacheStandardLessonTypes(lessonTypes)
        cacher.cacheStandardScheduledLessons(loadedLessons)

        val listener = weakListener.get()
        listener?.onLessonsLoaded(loadedLessons, lessonTypes, operationId)
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        val listener = weakListener.get()
        if (listener != null) {
            listener.onNetworkErrorHappened(error, operationId)
            sender.processRequestAfterTimeout(this)
        }
    }

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        val listener = weakListener.get()
        if (listener != null) {
            listener.onParsingErrorHappened(e, operationId)
            sender.processRequestAfterTimeout(this)
        }
    }

    override fun notifyOnBeforeSend() {
        val listener = weakListener.get()
        listener?.onLoadingMessageDispatched(StandardLessonsLoadingMessage(operationId, isARetry = false))
    }

}

internal class LessonTypesOfStudyCourseRequest(
        studyCourse: StudyCourse,
        val listener: ListLessonsListener) : BasicLessonRequest(studyCourse) {

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onParsingErrorHappened(e)
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onErrorHappened(error)
    }

    override fun notifyOnBeforeSend() {; }

    override fun onTeachingsAndLessonsLoaded(
            lessonTypes: List<LessonType>,
            loadedLessons: List<LessonSchedule>) {
        listener.onLessonTypesRetrieved(lessonTypes)
    }

}


internal abstract class BasicExtraLessonRequest(val extraCourse: ExtraCourse) : BasicLessonRequest(extraCourse.studyCourse) {

    final override fun onTeachingsAndLessonsLoaded(
            lessonTypes: List<LessonType>,
            loadedLessons: List<LessonSchedule>) {


        val searchedExtraLessons = loadedLessons.filter { it.lessonTypeId == extraCourse.lessonTypeId }
        val searchedLessonType = lessonTypes.first { it.id == extraCourse.lessonTypeId }

        onExtraCourseLessonsLoaded(searchedExtraLessons, searchedLessonType)
    }

    abstract fun onExtraCourseLessonsLoaded(
            loadedLessons: List<LessonSchedule>,
            lessonType: LessonType)
}

internal class LessonsOfExtraCourseRequest(
        extraCourse: ExtraCourse,
        listener: LessonsLoadingListener,
        private val cacher: Cacher
) : BasicExtraLessonRequest(extraCourse) {

    private val weakListener = WeakReference(listener)

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        val listener = weakListener.get()
        if (listener != null) {
            listener.onParsingErrorHappened(e, operationId)
            sender.processRequestAfterTimeout(this)
        }
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        val listener = weakListener.get()
        if (listener != null) {
            listener.onNetworkErrorHappened(error, operationId)
            sender.processRequestAfterTimeout(this)
        }
    }

    override fun notifyOnBeforeSend() {
        weakListener.get()?.onLoadingMessageDispatched(ExtraLessonsLoadingMessage(extraCourse, operationId))
    }

    override fun onExtraCourseLessonsLoaded(
            loadedLessons: List<LessonSchedule>,
            lessonType: LessonType) {
        cacher.cacheExtraScheduledLessons(loadedLessons)

        weakListener.get()?.onLessonsLoaded(loadedLessons, listOf(lessonType), operationId)
    }

}

/**
 * Requests the lessons from the network and tries to find differences between the fetched lesson
 * and the cached ones.
 * @param lastValidTimestamp the changes occurring after this date will not be dispatched to the
 * listener
 */
internal class DiffStudyCourseRequest(
        course: StudyCourse,
        private val listener: DiffLessonsListener,
        private val cachedLessons: List<LessonSchedule>,
        private val cacher: Cacher,
        private val lastValidTimestamp: Long?) : BasicLessonRequest(course) {

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onBeforeRequestFinished()
        listener.onLessonsLoadingError()
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onBeforeRequestFinished()
        listener.onLessonsLoadingError()
    }

    override fun onTeachingsAndLessonsLoaded(
            lessonTypes: List<LessonType>,
            loadedLessons: List<LessonSchedule>) {
        //caching results
        cacher.cacheStandardLessonTypes(lessonTypes)
        cacher.cacheStandardScheduledLessons(loadedLessons)

        //performing the diff
        val loadedList = ArrayList<LessonSchedule>(loadedLessons)
        val cachedList = ArrayList<LessonSchedule>(cachedLessons)

        val diffedLessons = LessonSchedule.diffLessons(cachedList, loadedList)
        diffedLessons.discardPassedChanges()
        if (lastValidTimestamp != null) {
            diffedLessons.discardChangesAfterTimestamp(lastValidTimestamp)
        }

        listener.onBeforeRequestFinished()
        listener.onLessonsDiffed(diffedLessons)
    }

}

internal class DiffExtraCourseRequest(
        course: ExtraCourse,
        val listener: DiffLessonsListener,
        private val cachedLessons: List<LessonSchedule>,
        private val cacher: Cacher,
        private val lastValidTimestamp: Long?) : BasicExtraLessonRequest(course) {

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onBeforeRequestFinished()
        listener.onLessonsLoadingError()
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onBeforeRequestFinished()
        listener.onLessonsLoadingError()
    }

    override fun onExtraCourseLessonsLoaded(
            loadedLessons: List<LessonSchedule>,
            lessonType: LessonType) {
        //caching results
        cacher.cacheExtraScheduledLessons(loadedLessons)

        //performing the diff
        val loadedList = ArrayList<LessonSchedule>(loadedLessons)
        val cachedList = ArrayList<LessonSchedule>(cachedLessons)

        val diffedLessons = LessonSchedule.diffLessons(cachedList, loadedList)
        if (lastValidTimestamp != null) {
            diffedLessons.discardChangesAfterTimestamp(lastValidTimestamp)
        }

        listener.onBeforeRequestFinished()
        listener.onLessonsDiffed(diffedLessons)
    }

}
