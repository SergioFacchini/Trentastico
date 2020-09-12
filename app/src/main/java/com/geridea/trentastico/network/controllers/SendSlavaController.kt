package com.geridea.trentastico.network.controllers


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import com.geridea.trentastico.BuildConfig
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener
import com.geridea.trentastico.network.request.IRequest
import com.geridea.trentastico.network.request.RequestSender
import com.geridea.trentastico.utils.AppPreferences
import okhttp3.FormBody

class SendSlavaController(private val sender: RequestSender) {

    fun sendFeedback(feedback: String, name: String, email: String, listener: FeedbackSendListener)
            = sender.processRequest(SendFeedbackRequest(feedback, name, email, listener))

    fun sendDonation(itemName: String, who: String) {
        sender.processRequest(SendDonationRequest(itemName, who))
    }

    private open class SendFeedbackRequest(private val feedback: String, private val name: String, private val email: String, private val listener: FeedbackSendListener) : IRequest {
        override fun notifyNetworkProblem(error: Exception, sender: RequestSender) {
            listener.onErrorHappened()
        }

        override fun notifyResponseProcessingFailure(e: Exception, sender: RequestSender) = listener.onErrorHappened()

        override fun manageResponse(responseToManage: String, sender: RequestSender) = if (responseToManage == "OK") {
            listener.onFeedbackSent()
        } else {
            listener.onErrorHappened()
        }

        override fun notifyOnBeforeSend() = //Nothing to do
                Unit

        override val url: String
            get() = "https://ideagenesi.com/trentastico/submit_idea.php"

        override val formToSend: FormBody?
            get() = FormBody.Builder()
                    .add("email", email)
                    .add("name", name)
                    .add("feedback", feedback)
                    .add("android-id", AppPreferences.androidId)
                    .add("app-version", "(" + BuildConfig.VERSION_CODE + ") " + BuildConfig.VERSION_NAME)
                    .build()

    }

    private class SendDonationRequest(itemName: String, who: String):
            SendFeedbackRequest("Donazione $itemName", who, "", NullFeedbackListener())

    class NullFeedbackListener : FeedbackSendListener {
        override fun onErrorHappened() {}

        override fun onFeedbackSent() {}

    }

}
