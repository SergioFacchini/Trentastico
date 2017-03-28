package com.geridea.trentastico.gui.views.requestloader;


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

public class TerminalMessage implements ILoadingMessage {

    private final int id;

    public TerminalMessage(int id) {
        this.id = id;
    }

    @Override
    public void process(RequestLoaderView requestLoaderView) {
        requestLoaderView.removeMessage(getMessageId());
    }

    public int getMessageId() {
        return id;
    }
}
