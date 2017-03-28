package com.geridea.trentastico.network.operations;

import com.geridea.trentastico.gui.views.requestloader.AbstractTextMessage;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
public class ParsingErrorMessage extends AbstractTextMessage {

    public ParsingErrorMessage(int messageId) {
        super(messageId);
    }

    @Override
    public String getText() {
        return "Non sono riuscito ad interpretare gli orari. Riprovo a scaricarli...";
    }
}
