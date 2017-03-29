package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

public interface IRequest {
    void notifyFailure(Exception e, RequestSender sender);
    void manageResponse(String string, RequestSender sender);
    void notifyResponseUnsuccessful(int code, RequestSender sender);
    void notifyOnBeforeSend();

    String getURL();
}
