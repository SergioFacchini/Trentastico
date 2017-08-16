package com.geridea.trentastico.network.request


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import okhttp3.FormBody

interface IRequest {
    fun notifyFailure(e: Exception, sender: RequestSender)
    fun manageResponse(responseToManage: String, sender: RequestSender)
    fun notifyResponseUnsuccessful(code: Int, sender: RequestSender)
    fun notifyOnBeforeSend()

    val url: String

    /**
     *
     * @return String to send, or null if the request has nothing to send
     */
    val formToSend: FormBody?
}
