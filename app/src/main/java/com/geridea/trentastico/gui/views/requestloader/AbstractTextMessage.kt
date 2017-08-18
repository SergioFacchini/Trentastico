package com.geridea.trentastico.gui.views.requestloader


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

abstract class AbstractTextMessage(val messageId: Int) : ILoadingMessage {

    override fun process(requestLoaderView: RequestLoaderView) =
            requestLoaderView.addOrReplaceMessage(this)

    abstract val text: String

    override fun toString(): String = "[$messageId: $text]"

}
