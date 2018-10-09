package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 18/04/2017.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener
import com.geridea.trentastico.utils.UIUtils
import kotlinx.android.synthetic.main.fragment_submit_bug.*

class SubmitFeedbackFragment : FragmentWithMenuItems() {

    private var isFeedbackBeingSent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_submit_bug, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        send_button.setOnClickListener {
            if (isFeedbackBeingSent) {
                return@setOnClickListener
            }

            val feedback = feedbackText.text.toString().trim { it <= ' ' }
            if (feedback.isEmpty()) {
                Toast.makeText(activity, "Per favore inserisci il feedback.", Toast.LENGTH_SHORT).show()
            } else {
                isFeedbackBeingSent = true

                val name = nameText.text.toString()
                val email = emailText.text.toString()

                Networker.sendFeedback(feedback, name, email, object : FeedbackSendListener {

                    override fun onFeedbackSent() {
                        isFeedbackBeingSent = false

                        UIUtils.showToastOnMainThread(
                                activity!!,
                                "Grazie per avermi inviato il tuo feedback. Lo apprezzo molto! :)"
                        )

                        goToCalendarFragment()
                    }

                    override fun onErrorHappened() {
                        isFeedbackBeingSent = false

                        UIUtils.showToastOnMainThread(
                                activity!!,
                                "Si è verificato un errore durante il tentativo di invio del feedback!"
                        )
                    }
                })
            }

        }
    }


    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //We're not using any menu item
            Unit

}
