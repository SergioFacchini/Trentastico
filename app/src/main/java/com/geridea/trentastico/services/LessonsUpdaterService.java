package com.geridea.trentastico.services;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import com.threerings.signals.Listener1;
import com.threerings.signals.Signal1;

import java.util.Calendar;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class LessonsUpdaterService extends Service {

    public static final String EXTRA_STARTER = "EXTRA_STARTER";

    public static final int STARTER_UNKNOWN = 0;
    public static final int STARTER_NETWORK_BROADCAST = 1;
    public static final int STARTER_BOOT_BROADCAST = 2;
    public static final int STARTER_APP_START = 3;
    public static final int STARTER_ALARM_MANAGER = 4;

    public static final int SCHEDULE_SLOW = 1;
    public static final int SCHEDULE_QUICK = 2;
    public static final int SCHEDULE_MISSING = 3;

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
        //Check for lessons updates if
        // 1) More than 4 hours have passed since last check
        // 2) In the last check we had no internet and now we have it

        //Rescheduling of the lessons
        // 1) The check has been successful:
        //    Just reschedule at the normal pace
        // 2) We had no internet:
        //    We reschedule the check and check on the connectivity change broadcast.
        // 3) The check was unsuccessful:
        //    Reschedule at a slower rate.
        if (updateAlreadyInProgress) {
            //The service is already started and it's doing something
            showToastIfInDebug("Update already in progress... ignoring update.");
            return START_REDELIVER_INTENT;
        } else {
            updateAlreadyInProgress = true;

            final int starter = intent.getIntExtra(EXTRA_STARTER, STARTER_UNKNOWN);
            if(shouldUpdateBecauseWeGainedInternet(starter)){
                showToastIfInDebug("Updating lessons because of internet refresh state...");
                AppPreferences.hadInternetInLastCheck(true);

                updateLessons(new LessonsUpdateListener() {
                    @Override
                    public void onLessonsUpdateTerminated(boolean successful) {
                        scheduleNextStartAndTerminate(successful ? SCHEDULE_SLOW : SCHEDULE_QUICK);
                    }
                });
            } else if (shouldUpdateBecauseOfUpdateTimeout()) {
                showToastIfInDebug("Checking for lessons updates...");
                updateLessons(new LessonsUpdateListener() {
                    @Override
                    public void onLessonsUpdateTerminated(boolean successful) {
                        scheduleNextStartAndTerminate(successful ? SCHEDULE_SLOW : SCHEDULE_QUICK);
                    }
                });
            } else {
                showToastIfInDebug("Too early to check for updates.");
                scheduleNextStartAndTerminate(SCHEDULE_MISSING);
            }
        }

        return START_REDELIVER_INTENT;
    }

    private boolean shouldUpdateBecauseWeGainedInternet(int starter) {
        return starter == STARTER_NETWORK_BROADCAST
            && !AppPreferences.hadInternetInLastCheck()
            && ContextUtils.weHaveInternet(this);
    }

    private boolean shouldUpdateBecauseOfUpdateTimeout() {
        return AppPreferences.getNextLessonsUpdateTime() <= System.currentTimeMillis();
    }


    private void showToastIfInDebug(String message) {
        if (Config.DEBUG_MODE) {
            showToastOnMainThread(message);
        }
    }

    private void scheduleNextStartAndTerminate(int scheduleType) {
        Calendar calendar = calculateAndSaveNextSchedule(scheduleType);

        Intent intent = createServiceIntent(this, STARTER_ALARM_MANAGER);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);

        stopSelf();
    }

    @NonNull
    private Calendar calculateAndSaveNextSchedule(int scheduleType) {
        Calendar calendar;
        if(scheduleType == SCHEDULE_MISSING) {
            //Posticipating due to alarm manager approximations
            calendar = CalendarUtils.getCalendarInitializedAs(AppPreferences.getNextLessonsUpdateTime());

            if(Config.DEBUG_MODE){
                calendar.add(Calendar.SECOND, Config.DEBUG_LESSONS_REFRESH_POSTICIPATION_SECONDS);
            } else {
                calendar.add(Calendar.MINUTE, Config.LESSONS_REFRESH_POSTICIPATION_MINUTES);
            }

        } else {
            calendar = CalendarUtils.getCalendarInitializedAs(System.currentTimeMillis());

            if (Config.DEBUG_MODE) {
                int timeToAdd = Config.DEBUG_LESSONS_REFRESH_WAITING_RATE_SECONDS;
                if (scheduleType == SCHEDULE_QUICK) timeToAdd /= 2;

                calendar.add(Calendar.SECOND, timeToAdd);
            } else {
                int timeToAdd = Config.LESSONS_REFRESH_WAITING_HOURS;
                if (scheduleType == SCHEDULE_QUICK) timeToAdd /= 2;

                calendar.add(Calendar.HOUR_OF_DAY, timeToAdd);
            }

            AppPreferences.setNextLessonsUpdateTime(calendar.getTimeInMillis());
        }
        return calendar;
    }

    @NonNull
    public static Intent createServiceIntent(Context context, int starterAlarmManager) {
        Intent intent = new Intent(context, LessonsUpdaterService.class);
        intent.putExtra(EXTRA_STARTER, starterAlarmManager);
        return intent;
    }

    private void updateLessons(final LessonsUpdateListener listener) {
        if (ContextUtils.weHaveInternet(this)) {
            if (AppPreferences.isStudyCourseSet()) {
                UpdateLessonsJob job = new UpdateLessonsJob();
                job.onCheckTerminated.connect(new Listener1<Boolean>() {
                    @Override
                    public void apply(Boolean isSuccessful) {
                        if (isSuccessful) {
                            showToastIfInDebug("Lesson update successful!");
                        } else {
                            showToastIfInDebug("Lesson update happened with error!");
                        }

                        listener.onLessonsUpdateTerminated(isSuccessful);
                    }
                });
                jobManager.addJobInBackground(job);
            } else {
                //The user has just run the app or reset it's settings. We currently do not have any
                //study course to fetch lessons from, so we just re-plan the check to the next time.
                listener.onLessonsUpdateTerminated(false);
            }
        } else {
            showToastIfInDebug("No internet. Cannot check for updates.");
            AppPreferences.hadInternetInLastCheck(false);
            listener.onLessonsUpdateTerminated(false);
        }
    }

    private void showToastOnMainThread(final String message) {
        UIUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LessonsUpdaterService.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class UpdateLessonsJob extends Job implements LessonsLoadingListener {

        final Signal1<Boolean> onCheckTerminated = new Signal1<>();

        private int numRequestsSent, numRequestsSucceeded, numRequestsFailed;

        UpdateLessonsJob() {
            super(new Params(1));

            numRequestsSent = 0;
            numRequestsSucceeded = 0;
            numRequestsFailed = 0;
        }

        @Override
        public void onAdded() {
        }

        @Override
        public void onRun() throws Throwable {

            //Loading the current and the next week
            Networker.refreshLessonsCache(new WeekInterval(0, +1), this);
        }

        @Override
        protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        }

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
                if (numRequestsSent == numRequestsSucceeded) {
                    onCheckTerminated.dispatch(true);
                } else {
                    onCheckTerminated.dispatch(false);
                }
            }
        }

    }

    private interface LessonsUpdateListener {
        void onLessonsUpdateTerminated(boolean successful);
    }
}
