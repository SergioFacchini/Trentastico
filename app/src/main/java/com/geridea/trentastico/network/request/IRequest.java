package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.support.annotation.Nullable;

import okhttp3.FormBody;

public interface IRequest {
    void notifyFailure(Exception e, RequestSender sender);
    void manageResponse(String string, RequestSender sender);
    void notifyResponseUnsuccessful(int code, RequestSender sender);
    void notifyOnBeforeSend();

    String getURL();

    /**
     *
     * @return String to send, or null if the request has nothing to send
     */
    @Nullable
    FormBody getFormToSend();
}
