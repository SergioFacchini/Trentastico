package com.geridea.trentastico.gui.views.requestloader;


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;

import java.util.Iterator;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RequestLoaderView extends FrameLayout {

    @BindView(R.id.loading_text)     TextView loadingText;
    @BindView(R.id.loading_progress) TextView loadingProgress;

    private int maxMessagesSinceLastShow = 0;

    private View view;

    private final Vector<AbstractTextMessage> currentMessages = new Vector<>(10);

    public RequestLoaderView(@NonNull Context context) {
        super(context);
        init();
    }

    public RequestLoaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RequestLoaderView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        view = Views.inflate(this, R.layout.view_request_loader);
        ButterKnife.bind(this, view);

        addView(view);

        if (!isInEditMode()) {
            postRecalculateMessageToDisplay();
        }
    }

    void addOrReplaceMessage(AbstractTextMessage newMessage){
        currentMessages.add(newMessage);

        boolean wasMessageInserted = false;
        for (int i = 0; i < currentMessages.size(); i++) {
            AbstractTextMessage currentMessage = currentMessages.get(i);
            if (currentMessage.getMessageId() == newMessage.getMessageId()) {
                currentMessages.set(i, newMessage);
                wasMessageInserted = true;
                break;
            }
        }

        if (!wasMessageInserted) {
            currentMessages.add(newMessage);
        }

        maxMessagesSinceLastShow = Math.max(maxMessagesSinceLastShow, currentMessages.size());

        postRecalculateMessageToDisplay();
    }

    private void recalculateMessagesToDisplay() {
        if (currentMessages.isEmpty()) {
            view.setVisibility(GONE);
            hideAndResetCounter();
        } else {
            view.setVisibility(VISIBLE);
            if(currentMessages.size() == 1 && maxMessagesSinceLastShow <= 1){ //maxMessages can be 0 too here
                //We show the only message we have
                showMessage(currentMessages.firstElement());
                hideAndResetCounter();
            } else {
                //We show the first message and get the counter to show how many messages
                showMessage(currentMessages.firstElement());
                showCounter();
            }
        }
    }

    private void showCounter() {
        int progress = maxMessagesSinceLastShow-currentMessages.size();
        loadingProgress.setText("("+ progress +"/"+maxMessagesSinceLastShow+")");
        loadingProgress.setVisibility(VISIBLE);
    }

    private void hideAndResetCounter() {
        loadingProgress.setVisibility(GONE);
        maxMessagesSinceLastShow = 0;
    }

    private void showMessage(AbstractTextMessage message){
        loadingText.setText(message.getText());
    }

    synchronized void removeMessage(int messageId){
        Iterator<AbstractTextMessage> messages = currentMessages.iterator();
        while(messages.hasNext()){
            AbstractTextMessage message = messages.next();
            if (message.getMessageId() == messageId) {
                messages.remove();
                break;
            }
        }

        postRecalculateMessageToDisplay();
    }

    private void postRecalculateMessageToDisplay() {
        post(new Runnable() {
            @Override
            public void run() {
                recalculateMessagesToDisplay();
            }
        });
    }

    public void processMessage(ILoadingMessage message){
        message.process(this);
    }

}
