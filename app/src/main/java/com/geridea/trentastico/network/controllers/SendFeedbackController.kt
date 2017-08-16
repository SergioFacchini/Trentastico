package com.geridea.trentastico.network.controllers


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import com.geridea.trentastico.BuildConfig
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener
import com.geridea.trentastico.utils.AppPreferences

import okhttp3.FormBody

class SendFeedbackController(sender: RequestSender, cacher: Cacher) : BasicController(sender, cacher) {

    fun sendFeedback(feedback: String, name: String, email: String, listener: FeedbackSendListener) {
        sender.processRequest(SendFeedbackRequest(feedback, name, email, listener))
    }

    protected class SendFeedbackRequest(private val feedback: String, private val name: String, private val email: String, private val listener: FeedbackSendListener) : IRequest {

        override fun notifyFailure(e: Exception, sender: RequestSender) {
            listener.onErrorHappened()
        }

        override fun manageResponse(string: String, sender: RequestSender) {
            if (string == "OK") {
                listener.onFeedbackSent()
            } else {
                listener.onErrorHappened()
            }
        }

        override fun notifyResponseUnsuccessful(code: Int, sender: RequestSender) {
            listener.onErrorHappened()
        }

        override fun notifyOnBeforeSend() {
            //Nothing to do
        }

        override val url: String
            get() = "http://ideagenesi.com/trentastico/submit_idea.php"

        override val formToSend: FormBody?
            get() = FormBody.Builder()
                    .add("email", email)
                    .add("name", name)
                    .add("feedback", feedback)
                    .add("android-id", AppPreferences.androidId)
                    .add("app-version", "(" + BuildConfig.VERSION_CODE + ") " + BuildConfig.VERSION_NAME)
                    .build()

    }
}
