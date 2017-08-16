package com.geridea.trentastico.gui.views.requestloader

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
class ParsingErrorMessage(messageId: Int) : AbstractTextMessage(messageId) {

    override val text: String
        get() = "Non sono riuscito ad interpretare gli orari. Riprovo a scaricarli..."
}
