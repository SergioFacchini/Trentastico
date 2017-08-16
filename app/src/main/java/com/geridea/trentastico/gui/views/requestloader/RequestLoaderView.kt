package com.geridea.trentastico.gui.views.requestloader


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

import android.content.Context
import android.support.annotation.AttrRes
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R

import java.util.ArrayList
import java.util.Collections

import butterknife.BindView
import butterknife.ButterKnife

class RequestLoaderView : FrameLayout {

    @BindView(R.id.loading_text) internal var loadingText: TextView? = null
    @BindView(R.id.loading_progress) internal var loadingProgress: TextView? = null

    private var maxMessagesSinceLastShow = 0

    private var view: View? = null

    private val currentMessages = Collections.synchronizedList(ArrayList<AbstractTextMessage>())

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        view = Views.inflate(this, R.layout.view_request_loader)
        ButterKnife.bind(this, view!!)

        addView(view)

        if (!isInEditMode) {
            postRecalculateMessageToDisplay()
        }
    }

    internal fun addOrReplaceMessage(newMessage: AbstractTextMessage) {
        synchronized(currentMessages) {
            var wasMessageInserted = false
            for (i in currentMessages.indices) {
                val currentMessage = currentMessages[i]
                if (currentMessage.messageId == newMessage.messageId) {
                    currentMessages[i] = newMessage
                    wasMessageInserted = true
                    break
                }
            }

            if (!wasMessageInserted) {
                currentMessages.add(newMessage)
            }

            maxMessagesSinceLastShow = Math.max(maxMessagesSinceLastShow, currentMessages.size)
        }

        postRecalculateMessageToDisplay()
    }

    private fun recalculateMessagesToDisplay() {
        synchronized(currentMessages) {
            if (currentMessages.isEmpty()) {
                view!!.visibility = View.GONE
                hideAndResetCounter()
            } else {
                view!!.visibility = View.VISIBLE
                if (currentMessages.size == 1 && maxMessagesSinceLastShow <= 1) { //maxMessages can be 0 too here
                    //We show the only message we have
                    showMessage(currentMessages[0])
                    hideAndResetCounter()
                } else {
                    //We show the first message and get the counter to show how many messages
                    showMessage(currentMessages[0])
                    showCounter()
                }
            }
        }

    }

    private fun showCounter() {
        val loadedMessages = maxMessagesSinceLastShow - currentMessages.size
        val progress = (loadedMessages * 100 / maxMessagesSinceLastShow.toDouble()).toInt()
        loadingProgress!!.text = "($progress%)"
        loadingProgress!!.visibility = View.VISIBLE
    }

    private fun hideAndResetCounter() {
        loadingProgress!!.visibility = View.GONE
        maxMessagesSinceLastShow = 0
    }

    private fun showMessage(message: AbstractTextMessage) {
        loadingText!!.text = message.text
    }

    @Synchronized internal fun removeMessage(messageId: Int) {
        synchronized(currentMessages) {
            val messages = currentMessages.iterator()
            while (messages.hasNext()) {
                val message = messages.next()
                if (message.messageId == messageId) {
                    messages.remove()
                    break
                }
            }
        }

        postRecalculateMessageToDisplay()
    }

    private fun postRecalculateMessageToDisplay() {
        post { recalculateMessagesToDisplay() }
    }

    fun processMessage(message: ILoadingMessage) {
        message.process(this)
    }

    companion object {

        val LOADING_FROM_CACHE_OPERATION_ID = -1
    }

}
