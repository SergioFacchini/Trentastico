package com.geridea.trentastico.network.operations;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;
import com.geridea.trentastico.gui.views.requestloader.AbstractTextMessage;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
public class NetworkErrorMessage extends AbstractTextMessage {

    private final String errorMessage;

    public NetworkErrorMessage(int messageId, VolleyError error) {
        super(messageId);

        if (error instanceof NoConnectionError) {
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
