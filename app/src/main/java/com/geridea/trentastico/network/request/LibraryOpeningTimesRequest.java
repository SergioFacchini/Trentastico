package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 28/04/2017.
 */

import android.support.annotation.Nullable;

import com.geridea.trentastico.model.LibraryOpeningTimes;
import com.geridea.trentastico.network.request.listener.LibraryOpeningTimesListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;

public class LibraryOpeningTimesRequest implements IRequest {

    private final Calendar date;
    private final LibraryOpeningTimesListener listener;

    private static final SimpleDateFormat URL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public LibraryOpeningTimesRequest(Calendar date, LibraryOpeningTimesListener listener) {
        this.date = date;
        this.listener = listener;
    }

    @Override
    public void notifyFailure(Exception e, RequestSender sender) {
        listener.onOpeningTimesLoadingError();
    }

    @Override
    public void manageResponse(String string, RequestSender sender) {
        //Parsing the response is not so easy, mainly because there are no consistency between the
        //classes of the responses. For instance when retrieving regular times, we have the follwing
        //response:
        //<span class="sede-open-time" style="float:right;margin-right:10px;">08:00-23:45 </span>
        //However, when the library is close, the response is:
        //<span style="float:right;color:#ca3538;margin-right:2.5%;">chiuso</span>
        //What's missing here is the 'class="sede-open-time"' between the two responses.
        try {
            LibraryOpeningTimes openingTimes = new LibraryOpeningTimes();

            Pattern compile = Pattern.compile("(chiuso)|([0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2})");
            Matcher matcher = compile.matcher(string);

            if (!matcher.find()) { throw new RuntimeException("Could not parse library opening times!"); }
            openingTimes.timesBuc = matcher.group(0);

            if (!matcher.find()) { throw new RuntimeException("Could not parse library opening times!"); }
            openingTimes.timesCial = matcher.group(0);

            if (!matcher.find()) { throw new RuntimeException("Could not parse library opening times!"); }
            openingTimes.timesMesiano = matcher.group(0);

            if (!matcher.find()) { throw new RuntimeException("Could not parse library opening times!"); }
            openingTimes.timesPovo = matcher.group(0);

            if (!matcher.find()) { throw new RuntimeException("Could not parse library opening times!"); }
            openingTimes.timesPsicologia = matcher.group(0);

            listener.onOpeningTimesLoaded(openingTimes, date);
        } catch (Exception e){
            listener.onErrorParsingResponse(e);
        }
    }

    @Override
    public void notifyResponseUnsuccessful(int code, RequestSender sender) {
        listener.onOpeningTimesLoadingError();
    }

    @Override
    public void notifyOnBeforeSend() {
        //We don't mange it
    }

    @Override
    public String getURL() {
        String formattedDate = URL_DATE_FORMAT.format(date.getTime());
        return "http://www.biblioteca.unitn.it/orarihp?data="+formattedDate+"&lingua=it";
    }

    @Nullable
    @Override
    public FormBody getFormToSend() {
        return null;
    }
}
