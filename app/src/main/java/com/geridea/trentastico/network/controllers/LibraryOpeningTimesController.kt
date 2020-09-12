package com.geridea.trentastico.network.controllers


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.model.LibraryOpeningTimes
import com.geridea.trentastico.network.controllers.listener.CachedLibraryOpeningTimesListener
import com.geridea.trentastico.network.controllers.listener.LibraryOpeningTimesListener
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import okhttp3.FormBody
import java.util.*
import java.util.regex.Pattern

class LibraryOpeningTimesController(val sender: RequestSender, val cacher: Cacher) {

    fun getLibraryOpeningTimes(day: Calendar, listener: LibraryOpeningTimesListener) =
            //Trying to retrieve all the data from the cache. If unavailable, get it from network or
            //dead cache
            cacher.getCachedLibraryOpeningTimes(day, false, object : CachedLibraryOpeningTimesListener {

                override fun onCachedOpeningTimesFound(times: LibraryOpeningTimes)
                        = listener.onOpeningTimesLoaded(times, day)

                override fun onNoCachedOpeningTimes()
                        = sender.processRequest(LibraryOpeningTimesRequest(day, listener))

            })


    private inner class LibraryOpeningTimesRequest(private val date: Calendar, private val listener: LibraryOpeningTimesListener) : IRequest {
        override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
            listener.onOpeningTimesLoadingError()

            tryToFetchTimesFromDeadCache()
        }

        override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) {
            listener.onOpeningTimesLoadingError()

            tryToFetchTimesFromDeadCache()
        }

        override fun manageResponse(responseToManage: String, sender: RequestSender) = //Parsing the response is not so easy, mainly because there are no consistency between the
                //classes of the responses. For instance when retrieving regular times, we have the following
                //response:
                //<span class="sede-open-time" style="float:right;margin-right:10px;">08:00-23:45 </span>
                //However, when the library is close, the response is:
                //<span style="float:right;color:#ca3538;margin-right:2.5%;">chiuso</span>
                //What's missing here is the 'class="sede-open-time"' between the two responses.
                try {
                    val openingTimes = LibraryOpeningTimes()
                    openingTimes.day = LibraryOpeningTimes.formatDay(date)

                    val compile = Pattern.compile("(chiuso)|([0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2})")
                    val matcher = compile.matcher(responseToManage)

                    if (!matcher.find()) throw RuntimeException("Could not parse library opening times!")
                    openingTimes.timesBuc = matcher.group(0)

                    if (!matcher.find()) throw RuntimeException("Could not parse library opening times!")
                    openingTimes.timesCial = matcher.group(0)

                    if (!matcher.find()) throw RuntimeException("Could not parse library opening times!")
                    openingTimes.timesMesiano = matcher.group(0)

                    if (!matcher.find()) throw RuntimeException("Could not parse library opening times!")
                    openingTimes.timesPovo = matcher.group(0)

                    if (!matcher.find()) throw RuntimeException("Could not parse library opening times!")
                    openingTimes.timesPsicologia = matcher.group(0)

                    listener.onOpeningTimesLoaded(openingTimes, date)

                    cacher.cacheLibraryOpeningTimes(openingTimes)
                } catch (e: Exception) {
                    listener.onErrorParsingResponse(e)
                }

        private fun tryToFetchTimesFromDeadCache() = //We can't get fresh data right now. Let's try to fetch it from from dead cache.
                cacher.getCachedLibraryOpeningTimes(date, true, object : CachedLibraryOpeningTimesListener {
                    override fun onCachedOpeningTimesFound(times: LibraryOpeningTimes) =
                            listener.onOpeningTimesLoaded(times, date)

                    override fun onNoCachedOpeningTimes() = //We have nothing in dead cache. We just rethrow the error:
                            listener.onOpeningTimesLoadingError()
                })

        override fun notifyOnBeforeSend() = //We don't mange it
                Unit

        override val url: String
            get() {
                val formattedDate = LibraryOpeningTimes.formatDay(date)
                return "https://www.biblioteca.unitn.it/orarihp?data=$formattedDate&lingua=it"
            }

        override val formToSend: FormBody?
            get() = null

    }
}
