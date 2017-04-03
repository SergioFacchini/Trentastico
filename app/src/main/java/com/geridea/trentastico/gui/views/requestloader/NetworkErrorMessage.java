package com.geridea.trentastico.gui.views.requestloader;

import java.net.UnknownHostException;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
public class NetworkErrorMessage extends AbstractTextMessage {

    private final String errorMessage;

    public NetworkErrorMessage(int messageId, Exception error) {
        super(messageId);

        if (error instanceof UnknownHostException) {
            errorMessage = "Non riesco a scaricare gli orari perché non sei connesso/a ad internet!";
        } else {
            errorMessage = "Non sono riuscito a scaricare gli orari per un problema con internet. Riprovo...";
        }
    }

    @Override
    public String getText() {
        return errorMessage;
    }
}
