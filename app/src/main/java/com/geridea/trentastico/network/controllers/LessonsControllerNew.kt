package com.geridea.trentastico.network.controllers

import com.geridea.trentastico.Config
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.gui.views.requestloader.LessonsLoadingMessage
import com.geridea.trentastico.model.*
import com.geridea.trentastico.network.controllers.listener.CoursesLoadingListener
import com.geridea.trentastico.network.controllers.listener.LessonsLoadingListener
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.network.request.ServerResponseParsingException
import okhttp3.FormBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


/*
 * Created with ♥ by Slava on 18/08/2017.
 */

class LessonsControllerNew(sender: RequestSender, cacher: Cacher) : BasicController(sender, cacher) {

    /**
     * Loads from the website all the study courses and returns them through the listener
     */
    fun loadStudyCourses(listener: CoursesLoadingListener) {
        sender.processRequest(LoadStudyCoursesRequest(listener))
    }

    fun loadLessons( listener: LessonsLoadingListener, studyCourse: StudyCourse) {
        sender.processRequest(LoadStandardLessonsRequest(listener, studyCourse))
    }

}

internal abstract class BasicRequest : IRequest {

    internal val requestId = nextAvailableId

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
 * Requests that load the lessons of the current StudyCourse
 */
internal class LoadStandardLessonsRequest(val listener: LessonsLoadingListener, val studyCourse: StudyCourse): BasicRequest() {

    override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
        listener.onNetworkErrorHappened(error, requestId)
    }

    override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
        listener.onParsingErrorHappened(e, requestId)
    }

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
        listener.onLessonsLoaded(parseLessons(json), parseTeachings(json), requestId)
    }

    private fun parseTeachings(json: JSONObject): List<LessonTypeNew> {
        //To get a list of teaching we might do the followings:
        // * Make another request to obtain a list
        // * Scan all the items in search of the teachings
        //The second one looks easier to implement, so I'll follow that way
        val numLessons = json.getInt("contains_data")

        return (0 until numLessons)
                .map { json[it.toString()] as JSONObject }
                .distinctBy { it.getString("codice_insegnamento") }
                .map {
                    LessonTypeNew(
                            id = it.getString("codice_insegnamento"),
                            name = it.getString("nome_insegnamento"),
                            color = 0xFFFF0000.toInt()
                    )
                }
    }

    private fun parseLessons(json: JSONObject): List<LessonSchedule> {
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
            val endingMins = convertToMinutes(lessonJson.getString("ora_fine"))

            LessonSchedule(
                    id            = id,
                    lessonTypeId  = lessonJson.getString("codice_insegnamento"),
                    teachersNames = lessonJson.getString("docente"),
                    room          = lessonJson.getString("aula"),
                    subject       = lessonJson.getString("nome_insegnamento"),
                    color         = (0xFF7DA3E0).toInt(),
                    startsAt      = startTimestamp,
                    endsAt        = calculdateEndTimeOfLesson(startTimestamp, endingMins, startingMins)
            )
        }
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


    override fun notifyOnBeforeSend() {
        listener.onLoadingAboutToStart(LessonsLoadingMessage(requestId, isARetry = false))
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


