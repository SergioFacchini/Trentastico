package com.geridea.trentastico.gui.views.requestloader

import java.net.UnknownHostException

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
class NetworkErrorMessage(messageId: Int, error: Exception) : AbstractTextMessage(messageId) {

    override val text: String

    init {

        if (error is UnknownHostException) {
            text = "Non riesco a scaricare gli orari perché non sei connesso/a ad internet!"
        } else {
            text = "Non sono riuscito a scaricare gli orari per un problema con internet. Riprovo..."
        }
    }
}
