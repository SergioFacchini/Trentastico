package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 18/04/2017.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast

import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener
import com.geridea.trentastico.utils.UIUtils

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick

class SubmitFeedbackFragment : FragmentWithMenuItems() {

    private var isFeedbackBeingSent = false

    @BindView(R.id.feedback_text) internal var feedbackText: EditText? = null
    @BindView(R.id.name_text) internal var nameText: EditText? = null
    @BindView(R.id.email_text) internal var emailText: EditText? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_submit_bug, container, false)
        ButterKnife.bind(this, view)

        return view
    }

    @OnClick(R.id.send_button)
    internal fun onSendButtonClick() {
        if (isFeedbackBeingSent) {
            return
        }

        val feedback = feedbackText!!.text.toString().trim { it <= ' ' }
        if (feedback.isEmpty()) {
            Toast.makeText(activity, "Per favore inserisci il feedback.", Toast.LENGTH_SHORT).show()
        } else {
            isFeedbackBeingSent = true

            val name = nameText!!.text.toString()
            val email = emailText!!.text.toString()

            Networker.sendFeedback(feedback, name, email, object : FeedbackSendListener {

                override fun onFeedbackSent() {
                    isFeedbackBeingSent = false

                    UIUtils.showToastOnMainThread(
                            activity,
                            "Grazie per avermi inviato il tuo feedback. Lo apprezzo molto! :)"
                    )

                    goToCalendarFragment()
                }

                override fun onErrorHappened() {
                    isFeedbackBeingSent = false

                    UIUtils.showToastOnMainThread(
                            activity,
                            "Si è verificato un errore durante il tentativo di invio del feedback!"
                    )
                }
            })
        }

    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) {
        //We're not using any menu item
    }

}
