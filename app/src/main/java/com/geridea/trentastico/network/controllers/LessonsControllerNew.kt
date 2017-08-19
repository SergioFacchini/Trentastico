package com.geridea.trentastico.network.controllers

import com.geridea.trentastico.Config
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.model.Course
import com.geridea.trentastico.model.StudyYear
import com.geridea.trentastico.network.controllers.listener.CoursesLoadingListener
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.network.request.ServerResponseParsingException
import okhttp3.FormBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern


/*
 * Created with ♥ by Slava on 18/08/2017.
 */

class LessonsControllerNew(
        sender: RequestSender,
        cacher: Cacher) : BasicController(sender, cacher) {

    internal class LoadStudyCoursesRequest(val listener: CoursesLoadingListener) : IRequest {

        private val jsonRegex = Pattern.compile("var elenco_corsi = ([^;]+?);\\svar")

        override val url: String
            get() = "https://easyroom.unitn.it/Orario/combo_call.php"

        override val formToSend: FormBody?
            get() = null

        override fun notifyFailure(e: Exception, sender: RequestSender) {
            if (e is ServerResponseParsingException) {
                listener.onParsingError(e)
            } else {
                listener.onLoadingError()
            }
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
            (0 until yearsJsonArray.length())
                    .map { yearsJsonArray[it] as JSONObject }
                    .filter { it.getString("valore") == Config.CURRENT_STUDY_YEAR }
                    .forEach { return it }

            throw ServerResponseParsingException("Cannot find current year JSON!")
        }

        override fun notifyResponseUnsuccessful(code: Int, sender: RequestSender) {
            listener.onLoadingError()
        }

        override fun notifyOnBeforeSend() { ; }

    }

    /**
     * Loads from the website all the study courses and returns them through the listener
     */
    fun loadStudyCourses(listener: CoursesLoadingListener) {
        sender.processRequest(LoadStudyCoursesRequest(listener))
    }

}
