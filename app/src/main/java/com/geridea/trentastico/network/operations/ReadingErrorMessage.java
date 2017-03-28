package com.geridea.trentastico.network.operations;

import com.geridea.trentastico.gui.views.requestloader.AbstractTextMessage;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
public class ReadingErrorMessage extends AbstractTextMessage {

    public ReadingErrorMessage(int messageId) {
        super(messageId);
    }

    @Override
    public String getText() {
        return "Non sono riuscito a scaricare gli orari. Hai una connessione internet attiva?";
    }
}
