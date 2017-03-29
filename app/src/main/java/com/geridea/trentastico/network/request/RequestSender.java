package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import com.geridea.trentastico.Config;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RequestSender {

    private final OkHttpClient client = new OkHttpClient();
    private final Vector<Call> callsInProgress = new Vector<>();

    private final Timer timeoutWaiter = new Timer();

    public void processRequest(IRequest requestToSend) {
        waitForDebuggingIfNeeded();

        Request request = new Request.Builder()
                .url(requestToSend.getURL())
                .build();

        requestToSend.notifyOnBeforeSend();

        Call call = client.newCall(request);
        call.enqueue(new RequestCallback(requestToSend));


        callsInProgress.add(call);
    }

    private void waitForDebuggingIfNeeded() {
        if (Config.DEBUG_MODE && Config.PRE_LOADING_WAITING_TIME_MS != 0) {
            try {
                Thread.sleep(Config.PRE_LOADING_WAITING_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void processRequestAfterTimeout(final IRequest requestToSend) {
        timeoutWaiter.schedule(new TimerTask() {
            @Override
            public void run() {
                processRequest(requestToSend);
            }
        }, Config.WAITING_TIME_AFTER_A_REQUEST_FAILED);
    }

    private class RequestCallback implements Callback {

        private IRequest request;

        RequestCallback(IRequest request) {
            this.request = request;
        }

        @Override public void onFailure(Call call, IOException e) {
            callsInProgress.remove(call);
            request.notifyFailure(e, RequestSender.this);
        }

        @Override public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                try {
                    ResponseBody responseBody = response.body();
                    String responseStr = responseBody.string();
                    responseBody.close();

                    request.manageResponse(responseStr, RequestSender.this);
                } catch (Exception e) {
                    request.notifyFailure(e, RequestSender.this);
                }
            } else {
                request.notifyResponseUnsuccessful(response.code(), RequestSender.this);
            }
            callsInProgress.remove(call);
        }
    }

}
