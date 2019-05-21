package com.geridea.trentastico.gui.views.requestloader

import java.net.ConnectException
import java.net.UnknownHostException

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
class NetworkErrorMessage(messageId: Int, error: Exception) : AbstractTextMessage(messageId) {

    override val text: String = when (error) {
        is UnknownHostException -> "Non riesco a scaricare gli orari perché non sei connesso/a ad internet!"
        is ConnectException     -> "Il sito degli orari sembra non funzionare. Riprovo..."
        else -> "Non sono riuscito a scaricare gli orari per un problema tecnico (${error.javaClass.name}). Riprovo..."
    }

}
