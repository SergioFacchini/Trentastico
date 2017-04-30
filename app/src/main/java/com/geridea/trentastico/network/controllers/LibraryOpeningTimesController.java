package com.geridea.trentastico.network.controllers;


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import android.support.annotation.Nullable;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.model.LibraryOpeningTimes;
import com.geridea.trentastico.network.request.IRequest;
import com.geridea.trentastico.network.request.RequestSender;
import com.geridea.trentastico.network.controllers.listener.CachedLibraryOpeningTimesListener;
import com.geridea.trentastico.network.controllers.listener.LibraryOpeningTimesListener;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;

public class LibraryOpeningTimesController extends BasicController {

    public LibraryOpeningTimesController(RequestSender sender, Cacher cacher) {
        super(sender, cacher);

    }

    public void getLibraryOpeningTimes(final Calendar day, final LibraryOpeningTimesListener listener) {
        //Trying to retrieve all the data from the cache. If unavailable, get it from network or
        //dead cache
        cacher.getCachedLibraryOpeningTimes(day, false, new CachedLibraryOpeningTimesListener(){
            @Override
            public void onCachedOpeningTimesFound(LibraryOpeningTimes times) {
                listener.onOpeningTimesLoaded(times, day);
            }

            @Override
            public void onNoCachedOpeningTimes() {
                sender.processRequest(new LibraryOpeningTimesRequest(day, listener));
            }
        });

    }


    protected class LibraryOpeningTimesRequest implements IRequest {

        private final Calendar date;
        private final LibraryOpeningTimesListener listener;

        public LibraryOpeningTimesRequest(Calendar date, LibraryOpeningTimesListener listener) {
            this.date = date;
            this.listener = listener;
        }

        @Override
        public void notifyFailure(Exception e, RequestSender sender) {
            listener.onOpeningTimesLoadingError();

            tryToFetchTimesFromDeadCache();
        }

        @Override
        public void manageResponse(String string, RequestSender sender) {
            //Parsing the response is not so easy, mainly because there are no consistency between the
            //classes of the responses. For instance when retrieving regular times, we have the following
            //response:
            //<span class="sede-open-time" style="float:right;margin-right:10px;">08:00-23:45 </span>
            //However, when the library is close, the response is:
            //<span style="float:right;color:#ca3538;margin-right:2.5%;">chiuso</span>
            //What's missing here is the 'class="sede-open-time"' between the two responses.
            try {
                LibraryOpeningTimes openingTimes = new LibraryOpeningTimes();
                openingTimes.day = LibraryOpeningTimes.formatDay(date);

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

                cacher.cacheLibraryOpeningTimes(openingTimes);
            } catch (Exception e){
                listener.onErrorParsingResponse(e);
            }
        }

        @Override
        public void notifyResponseUnsuccessful(int code, RequestSender sender) {
            listener.onOpeningTimesLoadingError();

            tryToFetchTimesFromDeadCache();
        }

        private void tryToFetchTimesFromDeadCache() {
            //We can't get fresh data right now. Let's try to fetch it from from dead cache.
            cacher.getCachedLibraryOpeningTimes(date, true, new CachedLibraryOpeningTimesListener() {
                @Override
                public void onCachedOpeningTimesFound(LibraryOpeningTimes times) {
                    listener.onOpeningTimesLoaded(times, date);
                }

                @Override
                public void onNoCachedOpeningTimes() {
                    //We have nothing in dead cache. We just rethrow the error:
                    listener.onOpeningTimesLoadingError();
                }
            });
        }

        @Override
        public void notifyOnBeforeSend() {
            //We don't mange it
        }

        @Override
        public String getURL() {
            String formattedDate = LibraryOpeningTimes.formatDay(date);
            return "http://www.biblioteca.unitn.it/orarihp?data="+formattedDate+"&lingua=it";
        }

        @Nullable
        @Override
        public FormBody getFormToSend() {
            return null;
        }

    }
}
