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

    private val timeoutWaiter = Timer()

    fun processRequest(requestToSend: IRequest) {

        waitForDebuggingIfNeeded()
        requestToSend.notifyOnBeforeSend()

        val call = client.newCall(buildRequest(requestToSend))
        call.enqueue(RequestCallback(requestToSend))
    }

    fun processAsyncRequest(requestToSend: IRequest){
        waitForDebuggingIfNeeded()
        requestToSend.notifyOnBeforeSend()

        try {
            val call = client.newCall(buildRequest(requestToSend))
            call.execute().use { response ->
                onProcessResponseWithRequest(response, requestToSend)
            }
        } catch (e: Exception) {
            requestToSend.notifyNetworkProblem(e, this)
        }
    }

    private fun onProcessResponseWithRequest(response: Response, request: IRequest) {
        if (response.isSuccessful) {
            try {
                val responseStr = response.body()!!.use { it.string() }

                request.manageResponse(responseStr, this@RequestSender)
            } catch (e: Exception) {
                if (IS_IN_DEBUG_MODE) {
                    e.printStackTrace()
                }
                request.notifyResponseProcessingFailure(e, this@RequestSender)
            }
        } else {
            request.notifyNetworkProblem(
                    ResponseUnsuccessfulException(response.code()),
                    this@RequestSender
            )
        }
    }

    private fun buildRequest(requestToSend: IRequest): Request {
        val builder = Request.Builder()
        builder.url(requestToSend.url)

        //If we have something to post, we'll get it here
        val formBodyToSend = requestToSend.formToSend
        if (formBodyToSend != null) {
            builder.post(formBodyToSend)
        }

        return builder.build()
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
            request.notifyNetworkProblem(e, this@RequestSender)
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            onProcessResponseWithRequest(response, request)
        }
    }

}
