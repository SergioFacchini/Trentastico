package com.geridea.trentastico.network.request


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import okhttp3.FormBody

interface IRequest {

    val url: String

    /**
     *
     * @return String to send, or null if the request has nothing to send
     */
    val formToSend: FormBody?

    /**
     * Called when there was an error while processing the response
     */
    fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender)

    fun manageResponse(responseToManage: String, sender: RequestSender)

    /**
     * Called when the request could not be executed due to cancellation, a connectivity problem or
     * timeout. Because networks can fail during an exchange, it is possible that the remote server
     * accepted the request before the failure.
     */
    fun notifyNetworkProblem(error: Exception, sender: RequestSender)
    fun notifyOnBeforeSend()

}
