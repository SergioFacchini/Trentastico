package com.geridea.trentastico.network.controllers

import com.geridea.trentastico.Config
import com.geridea.trentastico.database_new.CacherNew
import com.geridea.trentastico.gui.views.requestloader.ExtraLessonsLoadingMessage
import com.geridea.trentastico.gui.views.requestloader.StandardLessonsLoadingMessage
import com.geridea.trentastico.model.*
import com.geridea.trentastico.network.controllers.listener.CoursesLoadingListener
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.network.controllers.listener.ListLessonsListener
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.network.request.ServerResponseParsingException
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.orIfBlank
import okhttp3.FormBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


/*
 * Created with ♥ by Slava on 18/08/2017.
 */

class LessonsControllerNew(private val sender: RequestSender, private val cacher: CacherNew) {

    /**
     * Loads from the website all the study courses and returns them through the listener
     */
    fun loadStudyCourses(listener: CoursesLoadingListener) {
        sender.processRequest(LoadStudyCoursesRequest(listener))
    }

    fun loadStandardLessons(listener: LessonsLoadingListener, studyCourse: StudyCourse) {
        val cachedLessons = cacher.getStandardLessonSchedulesCacheIfAny()
        val cachedTypes   = cacher.getStandardLessonTypesCacheIfAny()

        if (cachedLessons.isNotEmpty() && cachedTypes.isNotEmpty()) {
            listener.onLessonsLoaded(cachedLessons, cachedTypes, -1)
        } else {
            //No cache available, we have to download the data
            sender.processRequest(LoadStandardLessonsRequest(studyCourse, listener, cacher))
        }
    }

    fun loadLessonTypesOfStudyCourse(studyCourse: StudyCourse, listener: ListLessonsListener) {
        sender.processRequest(LessonTypesOfStudyCourseRequest(studyCourse, listener))
    }

    fun loadExtraCourseLessons(lessonsLoader: LessonsLoadingListener, extraCourse: ExtraCourse) {
        val cachedLessons = cacher.fetchExtraScheduledLessons(extraCourse.lessonTypeId)
        if (cachedLessons.isNotEmpty()) {
            val lessonType = LessonTypeNew(extraCourse, !AppPreferences.isLessonTypeToHide(extraCourse.lessonTypeId))

            lessonsLoader.onLessonsLoaded(cachedLessons, arrayListOf(lessonType), -1)
        } else {
            //No cache
            sender.processRequest(LessonOfExtraCourseRequest(extraCourse, lessonsLoader, cacher))
        }

    }

}


internal abstract class BasicRequest : IRequest {

    internal val operationId = nextAvailableId

    companion object {
        private var nextAvailableId = 1
            get() = field++
    }

}

/**
 * Requests that load all the available courses
 */
internal class LoadStudyCoursesRequest(val listener: CoursesLoadingListener) : IRequest {

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
           json data. We want the json associated to the "elenco_corsi" variable
         */
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
            val courses = List(currentYearCourses.length()) {
                val courseJson = currentYearCourses[it] as JSONObject
                val studyYears = parseStudyYears(courseJson.getJSONArray("elenco_anni"))

                Course(
                    id         = courseJson.getString("valore"),
                    name       = courseJson.getString("label"),
                    studyYears = studyYears
                )
            }

            listener.onCoursesFetched(courses)

        } else {
            throw ServerResponseParsingException("The data javascript is not properly formatted!")
        }
    }

    private fun parseStudyYears(yearsJson: JSONArray): List<StudyYear> {
        return List(yearsJson.length(), {
            val yearJson = yearsJson[it] as JSONObject

            StudyYear(
                id   = yearJson.getString("valore"),
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
internal abstract class BasicLessonRequest(val studyCourse: StudyCourse): BasicRequest() {

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
        val teachings = parseTeachings(json)
        val teachingsColorMapping = mapOf(
                *teachings.map { Pair(it.id, it.color) }.toTypedArray()
        )

        val lessons = parseLessons(json, teachingsColorMapping)

        onTeachingsAndLessonsLoaded(teachings, lessons)
    }

    /**
     * Hook function that notifies that the loading has been completed
     */
    abstract fun onTeachingsAndLessonsLoaded(lessonTypes: List<LessonTypeNew>, lessons: List<LessonSchedule>)

    private fun parseTeachings(json: JSONObject): List<LessonTypeNew> {
        var colorProgressive = 0

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
                    LessonTypeNew(
                        id               = lessonTypeId,
                        name             = calculateTeachingWithoutPartitioning(teachingName),
                        teachersNames    = it.getString("docente").orIfBlank("(insegnate non specificato)"),
                        partitioningName = partitioningName,
                        color            = teachingsColors[colorProgressive++],
                        isVisible        = !AppPreferences.isLessonTypeToHide(lessonTypeId)
                    )
                }
                .sortedBy { it.name}
    }

    /**
     * @param json the json to parse
     * @param teachingsColors the mapping teaching-id to color
     */
    private fun parseLessons(json: JSONObject, teachingsColors: Map<String, Int>): List<LessonSchedule> {
        val numLessons = json.getInt("contains_data")

        return (0 until numLessons).map {
            val lessonJson = json[it.toString()] as JSONObject

            //The lesson schedules do not have any identifier. We introduce it by merging the lesson
            //code with the lesson's timestamp
            val id = lessonJson.getString("codice_insegnamento") + "@" + lessonJson.getLong("timestamp")

            //The timestamp in the json is already set to the hour of the start of the lesson, but
            //not in the Rome timezone, we need to adjust it by removing two hours from it.
            //In order to get the ending of the lesson, we need to sum to the timestamp the
            //difference the starting and ending time
            val startTimestamp = lessonJson.getLong("timestamp") * 1000 - TimeUnit.HOURS.toMillis(2)
            val startingMins = convertToMinutes(lessonJson.getString("ora_inizio"))
            val endingMins   = convertToMinutes(lessonJson.getString("ora_fine"))

            val teachingName = lessonJson.getString("nome_insegnamento")

            LessonSchedule(
                    id               = id,
                    lessonTypeId     = lessonJson.getString("codice_insegnamento"),
                    teachersNames    = lessonJson.getString("docente"),
                    room             = lessonJson.getString("aula"),
                    subject          = calculateTeachingWithoutPartitioning(teachingName),
                    partitioningName = calculatePartitioningName(teachingName),
                    color            = teachingsColors[lessonJson.getString("codice_insegnamento")]!!,
                    startsAt         = startTimestamp,
                    endsAt           = calculdateEndTimeOfLesson(startTimestamp, endingMins, startingMins)
            )
        }
    }

    private fun calculateTeachingWithoutPartitioning(teachingName: String): String =
            //The name of the partitioning is contained in the name of the course and is always
            //preceded by a dash with spaces " - ".
            teachingName.take(teachingName.lastIndexOf(" - "))

    private fun calculatePartitioningName(teachingName: String): String? {
        //The name of the partitioning is contained in the name of the course and is always
        //preceded by a dash with spaces " - ".
        val partitioningName = teachingName.takeLastWhile { it != '-' }.trim()
        return if (partitioningName == "nessun partizionamento") null else partitioningName
    }

    private fun calculdateEndTimeOfLesson( startTimestamp: Long, endingMins: Int, startingMins: Int): Long {
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
            val hours   = matcher.group(1).toInt()
            val minutes = matcher.group(2).toInt()

            return hours*60 + minutes
        } else {
            throw IllegalArgumentException("The minutes to convert must be in \"hh:mm\" format!")
        }
    }

    override val url: String
        get() = "https://easyroom.unitn.it/Orario/list_call.php"

    override val formToSend: FormBody?
        get() = FormBody.Builder()
                .add("form-type", "corso")
                .add("aa",    Config.CURRENT_STUDY_YEAR)
                .add("anno",  Config.CURRENT_STUDY_YEAR)
                .add("corso", studyCourse.courseId)
                .add("anno2", studyCourse.yearId)
                .build()

}

/**
 * Requests that load the lessons of the current StudyCourse
 */
internal class LoadStandardLessonsRequest(
        studyCourse: StudyCourse,
        val listener: LessonsLoadingListener,
        val cacher: CacherNew
): BasicLessonRequest(studyCourse) {

    override fun onTeachingsAndLessonsLoaded(lessonTypes: List<LessonTypeNew>, lessons: List<LessonSchedule>) {
        cacher.cacheStandardLessonTypes(lessonTypes)
        cacher.cacheStandardScheduledLessons(lessons)

        listener.onLessonsLoaded(lessons, lessonTypes, operationId)
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onNetworkErrorHappened(error, operationId)
    }

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onParsingErrorHappened(e, operationId)
    }

    override fun notifyOnBeforeSend() {
        listener.onLoadingAboutToStart(StandardLessonsLoadingMessage(operationId, isARetry = false))
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

    override fun notifyOnBeforeSend() { ; }

    override fun onTeachingsAndLessonsLoaded(lessonTypes: List<LessonTypeNew>, lessons: List<LessonSchedule>) {
        listener.onLessonTypesRetrieved(lessonTypes)
    }

}

internal class LessonOfExtraCourseRequest(
        private val extraCourse: ExtraCourse,
        val listener: LessonsLoadingListener,
        val cacher: CacherNew
): BasicLessonRequest(extraCourse.studyCourse) {

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onParsingErrorHappened(e, operationId)
    }

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onNetworkErrorHappened(error, operationId)
    }

    override fun notifyOnBeforeSend() {
        listener.onLoadingAboutToStart(ExtraLessonsLoadingMessage(extraCourse, operationId))
    }

    override fun onTeachingsAndLessonsLoaded(lessonTypes: List<LessonTypeNew>, lessons: List<LessonSchedule>) {
        val searchedExtraLessons = lessons.filter { it.lessonTypeId == extraCourse.lessonTypeId }
        val searchedLessonType        = lessonTypes.first { it.id == extraCourse.lessonTypeId }

        cacher.cacheExtraScheduledLessons(searchedExtraLessons)

        listener.onLessonsLoaded(searchedExtraLessons, listOf(searchedLessonType), operationId)
    }

}

private val teachingsColors = arrayOf(
        0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(), 0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(),
        0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(), 0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(),
        0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(), 0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(),
        0xFF80D886.toInt(), 0xFFA6E2FF.toInt(), 0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(),
        0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(), 0xFFDC8080.toInt(), 0xFFFFB480.toInt(),
        0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(), 0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(),
        0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(), 0xFF56C878.toInt(), 0xFFE9D6B2.toInt(),
        0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(), 0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(),
        0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(), 0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(),
        0xFFBFCFCF.toInt(), 0xFF59B726.toInt(), 0xFFDE7083.toInt(), 0xFF68BBD9.toInt(),
        0xFFFF8147.toInt(), 0xFF87D21E.toInt(), 0xFF44D36F.toInt(), 0xFFCA9D00.toInt(),
        0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(), 0xFF00BD09.toInt(), 0xFF4EC300.toInt(),
        0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(), 0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(),
        0xFFFF8D01.toInt(), 0xFFCAC300.toInt(), 0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(),
        0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(), 0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(),
        0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(), 0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(),
        0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(), 0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(),
        0xFF29D984.toInt(), 0xFF99FF99.toInt(), 0xFF9999FF.toInt(), 0xFF47BAAD.toInt(),
        0xFFFF9FFA.toInt(), 0xFF00C161.toInt(), 0xFFB093FF.toInt(), 0xFF57B3FF.toInt(),
        0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(), 0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(),
        0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(), 0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(),
        0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(), 0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(),
        0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(), 0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(),
        0xFFFFA3FF.toInt(), 0xFFC5FFB1.toInt(), 0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(),
        0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(), 0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(),
        0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(), 0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(),
        0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(), 0xFF80D886.toInt(), 0xFFA6E2FF.toInt(),
        0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(), 0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(),
        0xFFDC8080.toInt(), 0xFFFFB480.toInt(), 0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(),
        0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(), 0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(),
        0xFF56C878.toInt(), 0xFFE9D6B2.toInt(), 0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(),
        0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(), 0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(),
        0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(), 0xFFBFCFCF.toInt(), 0xFF59B726.toInt(),
        0xFFDE7083.toInt(), 0xFF68BBD9.toInt(), 0xFFFF8147.toInt(), 0xFF87D21E.toInt(),
        0xFF44D36F.toInt(), 0xFFCA9D00.toInt(), 0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(),
        0xFF00BD09.toInt(), 0xFF4EC300.toInt(), 0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(),
        0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(), 0xFFFF8D01.toInt(), 0xFFCAC300.toInt(),
        0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(), 0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(),
        0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(), 0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(),
        0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(), 0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(),
        0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(), 0xFF29D984.toInt(), 0xFF99FF99.toInt(),
        0xFF9999FF.toInt(), 0xFF47BAAD.toInt(), 0xFFFF9FFA.toInt(), 0xFF00C161.toInt(),
        0xFFB093FF.toInt(), 0xFF57B3FF.toInt(), 0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(),
        0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(), 0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(),
        0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(), 0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(),
        0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(), 0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(),
        0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(), 0xFFFFA3FF.toInt(), 0xFFC5FFB1.toInt(),
        0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(), 0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(),
        0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(), 0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(),
        0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(), 0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(),
        0xFF80D886.toInt(), 0xFFA6E2FF.toInt(), 0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(),
        0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(), 0xFFDC8080.toInt(), 0xFFFFB480.toInt(),
        0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(), 0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(),
        0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(), 0xFF56C878.toInt(), 0xFFE9D6B2.toInt(),
        0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(), 0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(),
        0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(), 0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(),
        0xFFBFCFCF.toInt(), 0xFF59B726.toInt(), 0xFFDE7083.toInt(), 0xFF68BBD9.toInt(),
        0xFFFF8147.toInt(), 0xFF87D21E.toInt(), 0xFF44D36F.toInt(), 0xFFCA9D00.toInt(),
        0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(), 0xFF00BD09.toInt(), 0xFF4EC300.toInt(),
        0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(), 0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(),
        0xFFFF8D01.toInt(), 0xFFCAC300.toInt(), 0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(),
        0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(), 0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(),
        0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(), 0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(),
        0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(), 0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(),
        0xFF29D984.toInt(), 0xFF99FF99.toInt(), 0xFF9999FF.toInt(), 0xFF47BAAD.toInt(),
        0xFFFF9FFA.toInt(), 0xFF00C161.toInt(), 0xFFB093FF.toInt(), 0xFF57B3FF.toInt(),
        0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(), 0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(),
        0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(), 0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(),
        0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(), 0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(),
        0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(), 0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(),
        0xFFFFA3FF.toInt(), 0xFFC5FFB1.toInt(), 0xFFFFFCB1.toInt(), 0xFFFFE6BB.toInt(),
        0xFFB9F4FF.toInt(), 0xFFF3BAF5.toInt(), 0xFFF1D0D0.toInt(), 0xFFD5FFA4.toInt(),
        0xFFFDCBFE.toInt(), 0xFFA0F3A2.toInt(), 0xFFFFE8A4.toInt(), 0xFFFFC6A4.toInt(),
        0xFFEEC0C0.toInt(), 0xFFA7C7D3.toInt(), 0xFF80D886.toInt(), 0xFFA6E2FF.toInt(),
        0xFFDDFF75.toInt(), 0xFFFFBFCF.toInt(), 0xFF8CB1FF.toInt(), 0xFFFEE080.toInt(),
        0xFFDC8080.toInt(), 0xFFFFB480.toInt(), 0xFFA9F580.toInt(), 0xFFA6F9A2.toInt(),
        0xFFC6DBE1.toInt(), 0xFFF99ECD.toInt(), 0xFFC5C6FC.toInt(), 0xFFFDC100.toInt(),
        0xFF56C878.toInt(), 0xFFE9D6B2.toInt(), 0xFFFFA100.toInt(), 0xFFDBBEC9.toInt(),
        0xFFDB7BAE.toInt(), 0xFFFFFF72.toInt(), 0xFFF2CDA5.toInt(), 0xFFCABD00.toInt(),
        0xFFA9D8B9.toInt(), 0xFFEBB2BB.toInt(), 0xFFBFCFCF.toInt(), 0xFF59B726.toInt(),
        0xFFDE7083.toInt(), 0xFF68BBD9.toInt(), 0xFFFF8147.toInt(), 0xFF87D21E.toInt(),
        0xFF44D36F.toInt(), 0xFFCA9D00.toInt(), 0xFF5D94E1.toInt(), 0xFFCB78CE.toInt(),
        0xFF00BD09.toInt(), 0xFF4EC300.toInt(), 0xFFF99A9A.toInt(), 0xFFFFFF72.toInt(),
        0xFFC175D6.toInt(), 0xFF6BBAFF.toInt(), 0xFFFF8D01.toInt(), 0xFFCAC300.toInt(),
        0xFF5AA800.toInt(), 0xFFFF8F2E.toInt(), 0xFF3EC1A7.toInt(), 0xFFFF9C39.toInt(),
        0xFFE7698F.toInt(), 0xFFD9AC1A.toInt(), 0xFF3BCA8C.toInt(), 0xFFE06E96.toInt(),
        0xFFE9AF5D.toInt(), 0xFF6699CC.toInt(), 0xFFFF99CC.toInt(), 0xFF99CCFF.toInt(),
        0xFFDCB02C.toInt(), 0xFFFF7F5B.toInt(), 0xFF29D984.toInt(), 0xFF99FF99.toInt(),
        0xFF9999FF.toInt(), 0xFF47BAAD.toInt(), 0xFFFF9FFA.toInt(), 0xFF00C161.toInt(),
        0xFFB093FF.toInt(), 0xFF57B3FF.toInt(), 0xFFCAFF7A.toInt(), 0xFFEDE053.toInt(),
        0xFF5ECCB1.toInt(), 0xFF5AB8D2.toInt(), 0xFF7A95FF.toInt(), 0xFFFFD83D.toInt(),
        0xFFFF6A34.toInt(), 0xFF5AE09B.toInt(), 0xFFE4CA95.toInt(), 0xFF66C5BA.toInt(),
        0xFFCC7E7E.toInt(), 0xFFE6AFEE.toInt(), 0xFF8DD7B1.toInt(), 0xFFDE8EB6.toInt(),
        0xFFFFA3A3.toInt(), 0xFFFFD1A3.toInt(), 0xFFFFA3FF.toInt()
)
