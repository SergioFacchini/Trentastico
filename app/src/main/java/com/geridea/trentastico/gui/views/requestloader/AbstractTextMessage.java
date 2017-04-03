package com.geridea.trentastico.gui.views.requestloader;


/*
 * Created with ♥ by Slava on 28/03/2017.
 */

import com.geridea.trentastico.utils.time.CalendarInterval;
import com.geridea.trentastico.utils.time.WeekInterval;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public abstract class AbstractTextMessage implements ILoadingMessage {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM", Locale.ITALIAN);
    private final int messageId;

    public AbstractTextMessage(int messageId) {
        this.messageId = messageId;
    }

    protected static String formatFromToString(WeekInterval intervalToLoad) {
        CalendarInterval ci = intervalToLoad.toCalendarInterval();

        //Usually we're loading data that is exactly one or two weeks longs, considering the day
        //starting at 00:00:00. We don't want to show that day as loaded, so we back by a second
        //to the previous day.
        Calendar adjustedTo = (Calendar) ci.getTo().clone();
        adjustedTo.add(Calendar.SECOND, -1);

        return String.format(
                "%s al %s",
                DATE_FORMAT.format(ci.getFrom().getTime()),
                DATE_FORMAT.format(adjustedTo.getTime())
        );
    }

    @Override
    public final void process(RequestLoaderView requestLoaderView) {
        requestLoaderView.addOrReplaceMessage(this);
    }

    public abstract String getText();

    public int getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return "["+messageId+": "+getText()+"]";
    }
}
