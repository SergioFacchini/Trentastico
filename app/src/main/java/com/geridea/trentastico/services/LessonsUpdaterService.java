package com.geridea.trentastico.services;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.birbit.android.jobqueue.config.Configuration;
import com.geridea.trentastico.Config;
import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.activities.LessonsChangedActivity;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.network.request.LessonsDiffResult;
import com.geridea.trentastico.network.request.listener.LessonsDifferenceListener;
import com.geridea.trentastico.network.request.listener.WaitForDownloadLessonListener;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.ContextUtils;
import com.geridea.trentastico.utils.UIUtils;
import com.geridea.trentastico.utils.listeners.GenericListener1;
import com.geridea.trentastico.utils.time.CalendarUtils;
import com.geridea.trentastico.utils.time.WeekInterval;
import com.threerings.signals.Listener1;
import com.threerings.signals.Listener2;
import com.threerings.signals.Signal1;
import com.threerings.signals.Signal2;

import java.util.Calendar;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class LessonsUpdaterService extends Service {

    public static final String EXTRA_STARTER = "EXTRA_STARTER";

    public static final int STARTER_UNKNOWN = 0;
    public static final int STARTER_NETWORK_BROADCAST = 1;
    public static final int STARTER_BOOT_BROADCAST = 2;
    public static final int STARTER_APP_START = 3;
    public static final int STARTER_ALARM_MANAGER = 4;
    public static final int STARTER_SETTING_CHANGED = 5;
    public static final int STARTER_DEBUGGER = 6;

    public static final int SCHEDULE_SLOW = 1;
    public static final int SCHEDULE_QUICK = 2;
    public static final int SCHEDULE_MISSING = 3;
    public static final int NOTIFICATION_LESSONS_CHANGED_ID = 1000;

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
            return START_NOT_STICKY;
        } else if(AppPreferences.isSearchForLessonChangesEnabled()) {
            updateAlreadyInProgress = true;

            final int starter = intent.getIntExtra(EXTRA_STARTER, STARTER_UNKNOWN);
            if(shouldUpdateBecauseWeGainedInternet(starter)){
                showToastIfInDebug("Updating lessons because of internet refresh state...");
                AppPreferences.hadInternetInLastCheck(true);

                diffAndUpdateLessonsIfPossible(new LessonsDiffAndUpdateListener() {
                    @Override
                    public void onTerminated(boolean successful) {
                        scheduleNextStartAndTerminate(successful ? SCHEDULE_SLOW : SCHEDULE_QUICK);
                    }
                });
            } else if (shouldUpdateBecauseOfUpdateTimeout() || startedAppInDebugMode(starter)) {
                showToastIfInDebug("Checking for lessons updates...");
                diffAndUpdateLessonsIfPossible(new LessonsDiffAndUpdateListener() {
                    @Override
                    public void onTerminated(boolean successful) {
                        scheduleNextStartAndTerminate(successful ? SCHEDULE_SLOW : SCHEDULE_QUICK);
                    }
                });
            } else {
                showToastIfInDebug("Too early to check for updates.");
                scheduleNextStartAndTerminate(SCHEDULE_MISSING);
            }
        } else {
            showToastIfInDebug("Searching for lesson updates is disabled!");
        }

        return START_NOT_STICKY;
    }

    private boolean startedAppInDebugMode(int starter) {
        return (Config.DEBUG_MODE && starter == STARTER_APP_START) || (starter == STARTER_DEBUGGER);
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

        showToastIfInDebug("Scheduled alarm manager to "+CalendarUtils.formatTimestamp(calendar.getTimeInMillis()));

        stopSelf();
    }

    @NonNull
    private Calendar calculateAndSaveNextSchedule(int scheduleType) {
        Calendar calendar;
        if(scheduleType == SCHEDULE_MISSING) {
            //Postponing due to alarm manager approximations
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
    public static Intent createServiceIntent(Context context, int starter) {
        Intent intent = new Intent(context, LessonsUpdaterService.class);
        intent.putExtra(EXTRA_STARTER, starter);
        return intent;
    }

    private void diffAndUpdateLessonsIfPossible(final LessonsDiffAndUpdateListener listener) {
        if (ContextUtils.weHaveInternet(this)) {
            if (AppPreferences.isStudyCourseSet()) {
                //The current and next week
                final WeekInterval intervalToCheck = new WeekInterval(0, +1);

                diffLessons(intervalToCheck).connect(new Listener2<LessonsDiffResult, Boolean>() {
                    @Override
                    public void apply(LessonsDiffResult diffResult, final Boolean diffSuccessful) {
                        if (diffResult.isEmpty()) {
                            showToastIfInDebug("No lesson differences found.");
                        } else {
                            showLessonsChangedNotification(diffResult);
                        }

                        //We've tracked all the updates. Now we have to fetch the eventually missing
                        //schedules so we can be notified if they change in the future.
                        loadMissingLesson(intervalToCheck).connect(new Listener1<Boolean>(){
                            @Override
                            public void apply(Boolean updateSuccessful) {
                                listener.onTerminated(diffSuccessful && updateSuccessful);
                            }
                        });
                    }
                });
            } else {
                //The user has just run the app or reset it's settings. We currently do not have any
                //study course to fetch lessons from, so we just re-plan the check to the next time.
                listener.onTerminated(false);
            }
        } else {
            showToastIfInDebug("No internet. Cannot check for updates.");
            AppPreferences.hadInternetInLastCheck(false);
            listener.onTerminated(false);
        }
    }

    private void showLessonsChangedNotification(LessonsDiffResult diffResult) {
        if (AppPreferences.isNotificationForLessonChangesEnabled()) {
            int numDifferences = diffResult.getNumTotalDifferences();

            String message = "È cambiato l'orario di una lezione!";
            if (numDifferences > 1) {
                message = "Sono cambiati gli orari di "+numDifferences+" lezioni!";
            }


            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle(message)
                            .setContentText("Premi qui per i dettagli")
                            .setColor(getResources().getColor(R.color.colorPrimary))
                            .setAutoCancel(true);


            Intent intent = new Intent(this, LessonsChangedActivity.class);
            intent.putExtra(LessonsChangedActivity.EXTRA_DIFF_RESULT, diffResult);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            );

            notificationBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_LESSONS_CHANGED_ID, notificationBuilder.build());
        }
    }

    private Signal1<Boolean> loadMissingLesson(WeekInterval intervalToCheck) {
        LoadMissingLessonsJob job = new LoadMissingLessonsJob(intervalToCheck);
        jobManager.addJobInBackground(job);
        return job.onCheckTerminated;
    }

    private Signal2<LessonsDiffResult, Boolean> diffLessons(WeekInterval intervalToDiff) {
        DiffLessonsJob job = new DiffLessonsJob(intervalToDiff);
        jobManager.addJobInBackground(job);
        return job.onCheckTerminated;
    }

    private void showToastOnMainThread(final String message) {
        UIUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LessonsUpdaterService.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void cancelSchedules(Context context, int starter) {
        Intent serviceIntent = LessonsUpdaterService.createServiceIntent(
                context, starter
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getService(context,
                0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);
    }

    private class DiffLessonsJob extends Job implements LessonsDifferenceListener {

        final Signal2<LessonsDiffResult, Boolean> onCheckTerminated = new Signal2<>();

        private final WeekInterval intervalToDiff;
        private final LessonsDiffResult diffAccumulator;

        private int numRequestsSent, numRequestsSucceeded, numRequestsFailed;

        DiffLessonsJob(WeekInterval intervalToDiff) {
            super(new Params(1));

            this.numRequestsSent = 0;
            this.numRequestsSucceeded = 0;
            this.numRequestsFailed = 0;

            this.intervalToDiff = intervalToDiff;

            this.diffAccumulator = new LessonsDiffResult();
        }

        @Override
        public void onRun() throws Throwable {
            //Loading the current and the next week
            Networker.diffLessonsInCache(intervalToDiff, this);
        }

        @Override
        protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
            BugLogger.logBug();
            filterDiffAndDispatchCheckTerminated(false);
            return RetryConstraint.CANCEL;
        }

        @Override
        public void onRequestCompleted() {
            numRequestsSucceeded++;
            checkIfWeHaveFinished();
        }

        @Override
        public void onLoadingError() {
            numRequestsFailed++;
            checkIfWeHaveFinished();
        }

        @Override
        public void onNumberOfRequestToSendKnown(int numRequests) {
            numRequestsSent = numRequests;
        }

        public void checkIfWeHaveFinished() {
            if(numRequestsSent == numRequestsFailed + numRequestsSucceeded){
                boolean allSucceeded = numRequestsSent == numRequestsSucceeded;
                filterDiffAndDispatchCheckTerminated(allSucceeded);
            }
        }

        private void filterDiffAndDispatchCheckTerminated(boolean allSucceeded) {
            diffAccumulator.discardPastLessons();
            onCheckTerminated.dispatch(diffAccumulator, allSucceeded);
        }

        @Override
        public void onNoLessonsInCache() {
            //Nothing to diff!
            filterDiffAndDispatchCheckTerminated(false);
        }

        @Override
        public void onDiffResult(LessonsDiffResult lessonsDiffResult) {
            diffAccumulator.addFrom(lessonsDiffResult);
        }

        @Override
        protected void onCancel(int cancelReason, @Nullable Throwable throwable) { }

        @Override
        public void onAdded() { }
    }

    private class LoadMissingLessonsJob extends Job {

        final Signal1<Boolean> onCheckTerminated = new Signal1<>();

        private WeekInterval intervalToLoad;

        public LoadMissingLessonsJob(WeekInterval intervalToLoad) {
            super(new Params(1));

            this.intervalToLoad = intervalToLoad;
        }

        @Override
        public void onRun() throws Throwable {
            Networker.loadAndCacheNotCachedLessons(intervalToLoad,
                    new WaitForDownloadLessonListener(new GenericListener1<Boolean>() {

                @Override
                public void onFinish(Boolean success) {
                    onCheckTerminated.dispatch(success);
                }
            }));
        }

        @Override
        protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
            BugLogger.logBug();
            onCheckTerminated.dispatch(false);
            return RetryConstraint.CANCEL;
        }

        @Override
        public void onAdded() { }

        @Override
        protected void onCancel(int cancelReason, @Nullable Throwable throwable) { }
    }

    private interface LessonsDiffAndUpdateListener {
        void onTerminated(boolean successful);
    }
}
