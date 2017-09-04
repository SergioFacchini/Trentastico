package com.geridea.trentastico.gui.views.requestloader


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

open class TerminalMessage(private val messageId: Int) : ILoadingMessage {

    override fun process(requestLoaderView: RequestLoaderView) = requestLoaderView.removeMessage(messageId)
}
