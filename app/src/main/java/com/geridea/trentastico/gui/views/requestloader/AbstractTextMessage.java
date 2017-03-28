package com.geridea.trentastico.gui.views.requestloader;


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

public abstract class AbstractTextMessage implements ILoadingMessage {

    private final int messageId;

    public AbstractTextMessage(int messageId) {
        this.messageId = messageId;
    }

    @Override
    public final void process(RequestLoaderView requestLoaderView) {
        requestLoaderView.addOrReplaceMessage(this);
    }

    public abstract String getText();

    public int getMessageId() {
        return messageId;
    }
}
