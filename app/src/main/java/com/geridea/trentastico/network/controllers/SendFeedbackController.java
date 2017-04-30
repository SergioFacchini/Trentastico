package com.geridea.trentastico.network.controllers;


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import android.support.annotation.Nullable;

import com.geridea.trentastico.BuildConfig;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.network.controllers.listener.FeedbackSendListener;
import com.geridea.trentastico.utils.AppPreferences;

import okhttp3.FormBody;

public class SendFeedbackController extends BasicController {

    public SendFeedbackController(RequestSender sender, Cacher cacher) {
        super(sender, cacher);
    }

    public void sendFeedback(String feedback, String name, String email, FeedbackSendListener listener) {
        sender.processRequest(new SendFeedbackRequest(feedback, name, email, listener));
    }

    protected static class SendFeedbackRequest implements IRequest {
        private final String feedback;
        private final String name;
        private final String email;
        private final FeedbackSendListener listener;

        public SendFeedbackRequest(String feedback, String name, String email, FeedbackSendListener listener) {
            this.feedback = feedback;
            this.name = name;
            this.email = email;
            this.listener = listener;
        }

        @Override
        public void notifyFailure(Exception e, RequestSender sender) {
            listener.onErrorHappened();
        }

        @Override
        public void manageResponse(String string, RequestSender sender) {
            if (string.equals("OK")) {
                listener.onFeedbackSent();
            } else {
                listener.onErrorHappened();
            }
        }

        @Override
        public void notifyResponseUnsuccessful(int code, RequestSender sender) {
            listener.onErrorHappened();
        }

        @Override
        public void notifyOnBeforeSend() {
            //Nothing to do
        }

        @Override
        public String getURL() {
            return "http://ideagenesi.com/trentastico/submit_idea.php";
        }

        @Nullable
        @Override
        public FormBody getFormToSend() {
            return new FormBody.Builder()
                .add("email",    email)
                .add("name",     name)
                .add("feedback", feedback)
                .add("android-id", AppPreferences.getAndroidId())
                .add("app-version", "("+ BuildConfig.VERSION_CODE+") "+ BuildConfig.VERSION_NAME)
                .build();
        }

    }
}
