package com.geridea.trentastico.network.request


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.Config
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE
import okhttp3.*
import java.io.IOException
import java.util.*

class RequestSender {

    private val client = OkHttpClient()
    private val callsInProgress = Vector<Call>()

    private val timeoutWaiter = Timer()

    fun processRequest(requestToSend: IRequest) {
        waitForDebuggingIfNeeded()

        val builder = Request.Builder()
        builder.url(requestToSend.url)

        //If we have something to post, we'll get it here
        val formBodyToSend = requestToSend.formToSend
        if (formBodyToSend != null) {
            builder.post(formBodyToSend)
        }

        val request = builder.build()

        requestToSend.notifyOnBeforeSend()

        val call = client.newCall(request)
        call.enqueue(RequestCallback(requestToSend))


        callsInProgress.add(call)
    }

    private fun waitForDebuggingIfNeeded() {
        if (IS_IN_DEBUG_MODE && Config.PRE_LOADING_WAITING_TIME_MS != 0) {
            try {
                Thread.sleep(Config.PRE_LOADING_WAITING_TIME_MS.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    fun processRequestAfterTimeout(requestToSend: IRequest) = timeoutWaiter.schedule(object : TimerTask() {
        override fun run() = processRequest(requestToSend)
    }, Config.WAITING_TIME_AFTER_A_REQUEST_FAILED.toLong())

    private inner class RequestCallback internal constructor(private val request: IRequest) : Callback {

        override fun onFailure(call: Call, e: IOException) {
            callsInProgress.remove(call)
            request.notifyNetworkProblem(e, this@RequestSender)
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                try {
                    val responseBody = response.body()
                    val responseStr = responseBody.string()
                    responseBody.close()

                    request.manageResponse(responseStr, this@RequestSender)
                } catch (e: Exception) {
                    if (IS_IN_DEBUG_MODE) {
                        e.printStackTrace()
                    }
                    request.notifyResponseProcessingFailure(e, this@RequestSender)
                }

            } else {
                request.notifyNetworkProblem(ResponseUnsuccessfulException(response.code()), this@RequestSender)
            }
            callsInProgress.remove(call)
        }
    }

}
