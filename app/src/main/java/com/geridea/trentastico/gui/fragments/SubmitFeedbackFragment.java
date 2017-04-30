package com.geridea.trentastico.gui.fragments;


/*
 * Created with ♥ by Slava on 18/04/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener;
import com.geridea.trentastico.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SubmitFeedbackFragment extends FragmentWithMenuItems {

    private boolean isFeedbackBeingSent = false;

    @BindView(R.id.feedback_text) EditText feedbackText;
    @BindView(R.id.name_text)     EditText nameText;
    @BindView(R.id.email_text)    EditText emailText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submit_bug, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.send_button)
    void onSendButtonClick(){
        if (isFeedbackBeingSent) {
            return;
        }

        String feedback = feedbackText.getText().toString().trim();
        if (feedback.isEmpty()) {
            Toast.makeText(getActivity(), "Per favore inserisci il feedback.", Toast.LENGTH_SHORT).show();
        } else {
            isFeedbackBeingSent = true;

            String name  = nameText .getText().toString();
            String email = emailText.getText().toString();

            Networker.sendFeedback(feedback, name, email, new FeedbackSendListener(){

                @Override
                public void onFeedbackSent() {
                    isFeedbackBeingSent = false;

                    UIUtils.showToastOnMainThread(
                        getActivity(),
                        "Grazie per avermi inviato il tuo feedback. Lo apprezzo molto! :)"
                    );

                    goToCalendarFragment();
                }

                @Override
                public void onErrorHappened() {
                    isFeedbackBeingSent = false;

                    UIUtils.showToastOnMainThread(
                        getActivity(),
                        "Si è verificato un errore durante il tentativo di invio del feedback!"
                    );
                }
            });
        }

    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[0];
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        //We're not using any menu item
    }

}
