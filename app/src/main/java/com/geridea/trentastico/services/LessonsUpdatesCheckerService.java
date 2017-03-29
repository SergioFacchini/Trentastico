package com.geridea.trentastico.services;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.birbit.android.jobqueue.config.Configuration;
import com.geridea.trentastico.Config;
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.model.cache.CachedLessonsSet;
import com.geridea.trentastico.network.LessonsLoadingListener;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.ContextUtils;
import com.geridea.trentastico.utils.UIUtils;
import com.geridea.trentastico.utils.time.CalendarUtils;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Listener0;
import com.threerings.signals.Signal0;

import java.util.Calendar;

public class LessonsUpdatesCheckerService extends Service {

    private JobManager jobManager;

    private boolean updateAlreadyInProgress = false;

    @Override
    public void onCreate() {
        jobManager = new JobManager(new Configuration.Builder(this).build());

        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //Do not allow binding
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!updateAlreadyInProgress){
            updateAlreadyInProgress = true;

            long lastUpdate = AppPreferences.getLastLessonsUpdateTime();
            if (lastUpdate <= getLastPermittedNotUpdatePeriod()) {
                showToastIfInDebug("Checking for lessons updates...");
                updateLessons();
            } else {
                showToastIfInDebug("Too early to check for updates.");
            }
        } else {
            showToastOnMainThread("Update already in progress.. ignoring update.");
        }

        return START_REDELIVER_INTENT;
    }

    private void showToastIfInDebug(String message) {
        if (Config.DEBUG_MODE) {
            showToastOnMainThread(message);
        }
    }

    private void scheduleNextStartAndStop(boolean halfTime) {
        long lastUpdate = AppPreferences.getLastLessonsUpdateTime();
        Calendar calendar = CalendarUtils.getCalendarInitializedAs(lastUpdate);

        if(Config.DEBUG_MODE) {
            int timeToAdd = Config.DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS;
            if (halfTime) timeToAdd /= 2;
            
            calendar.add(Calendar.SECOND, timeToAdd);
        } else {
            int timeToAdd = Config.LESSONS_REFRESH_WAITING_HOURS;
            if (halfTime) timeToAdd /= 2;
            
            calendar.add(Calendar.HOUR_OF_DAY, timeToAdd);
        }

        Intent intent = new Intent(this, LessonsUpdatesCheckerService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);

        updateAlreadyInProgress = false;
        stopSelf();
    }

    private void updateLessons() {
        if(AppPreferences.isStudyCourseSet() && ContextUtils.weHaveInternet(this)){
            UpdateLessonsJob job = new UpdateLessonsJob();
            job.onRequestTerminatedSuccessfully.connect(new Listener0() {
                @Override
                public void apply() {
                    showToastIfInDebug("Lesson update successful!");
                    updateLastUpdateTime();
                    scheduleNextStartAndStop(false);
                }
            });
            job.onRequestTerminatedWithErrors.connect(new Listener0() {
                @Override
                public void apply() {
                    showToastIfInDebug("Lesson update happened with error!");
                    scheduleNextStartAndStop(true);
                }
            });
            jobManager.addJobInBackground(job);
        } else {
            //The user has just run the app or reset it's settings. We currently do not have any
            //study course to fetch lessons from, so we just re-plan the check to the next time.
            updateLastUpdateTime();
            scheduleNextStartAndStop(true);
        }
    }

    private void updateLastUpdateTime() {
        AppPreferences.setLastLessonsUpdateTime(System.currentTimeMillis());
    }

    private void showToastOnMainThread(final String message) {
        UIUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LessonsUpdatesCheckerService.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long getLastPermittedNotUpdatePeriod() {
        long currentMillis = System.currentTimeMillis();
        if (Config.DEBUG_MODE) {
            return currentMillis - CalendarUtils.SECONDS_MS * Config.DEBUG_LESSONS_REFRESH_RATE_SECONDS;
        } else {
            return currentMillis - CalendarUtils.HOUR_MS * Config.LESSONS_REFRESH_RATE_HOURS;
        }
    }

    private class UpdateLessonsJob extends Job implements LessonsLoadingListener {

        final Signal0 onRequestTerminatedSuccessfully = new Signal0();

        final Signal0 onRequestTerminatedWithErrors = new Signal0();

        private int numRequestsSent, numRequestsSucceeded, numRequestsFailed;

        UpdateLessonsJob() {
            super(new Params(1));

            numRequestsSent      = 0;
            numRequestsSucceeded = 0;
            numRequestsFailed    = 0;
        }

        @Override
        public void onAdded() { }

        @Override
        public void onRun() throws Throwable {

            //Loading the current and the next week
            Networker.refreshLessonsCache(new WeekInterval(0, +1), this);
        }

        @Override
        protected void onCancel(int cancelReason, @Nullable Throwable throwable) { }

        @Override
        protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
            return RetryConstraint.CANCEL;
        }

        ////////////////
        //Listener stuff

        @Override
        public void onLoadingAboutToStart(ILoadingMessage operation) {
            numRequestsSent++;
        }

        @Override
        public void onLessonsLoaded(LessonsSet lessonsSet, WeekInterval interval, int operationId) {
            numRequestsSucceeded++;
            checkIfWeHaveFinished();
        }

        @Override
        public void onErrorHappened(Exception error, int operationId) {
            //Managed in onLoadingAborted
        }

        @Override
        public void onParsingErrorHappened(Exception exception, int operationId) {
            //Managed in onLoadingAborted
        }

        @Override
        public void onLoadingDelegated(int operationId) {
            //Should never happen since we do not manage loading from cache
            BugLogger.logBug();
        }

        @Override
        public void onPartiallyCachedResultsFetched(CachedLessonsSet lessonsSet) {
            //Should never happen since we do not manage loading from cache
            BugLogger.logBug();
        }

        @Override
        public void onLoadingAborted(int operationId) {
            numRequestsFailed++;
            checkIfWeHaveFinished();
        }

        private void checkIfWeHaveFinished() {
            if (numRequestsSent == (numRequestsSucceeded + numRequestsFailed)) {
                if (numRequestsSent == numRequestsSucceeded){
                    onRequestTerminatedSuccessfully.dispatch();
                } else {
                    onRequestTerminatedWithErrors.dispatch();
                }
            }
        }

    }

}
