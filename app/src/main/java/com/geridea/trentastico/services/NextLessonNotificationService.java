package com.geridea.trentastico.services;


/*
 * Created with ♥ by Slava on 04/04/2017.
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

import com.geridea.trentastico.Config;
import com.geridea.trentastico.R;
import com.geridea.trentastico.database.TodaysLessonsListener;
import com.geridea.trentastico.gui.activities.FirstActivityChooserActivity;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.CalendarUtils;

import java.util.ArrayList;
import java.util.Calendar;

import static com.geridea.trentastico.utils.UIUtils.showToastIfInDebug;

public class NextLessonNotificationService extends Service {

    public static final int STARTER_DEBUG = -1;
    public static final int STARTER_PHONE_BOOT = 1;
    public static final int STARTER_NETWORK_BROADCAST = 2;
    public static final int STARTER_APP_BOOT = 3;
    public static final int STARTER_STUDY_COURSE_CHANGE = 4;
    public static final int STARTER_SWITCHED_ON = 5;

    public static final String EXTRA_STARTER = "EXTRA_STARTER";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!AppPreferences.isStudyCourseSet()) {
            //Probably first start. We have nothing to show to a user that doesn't have any lesson
            //planned!
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!AppPreferences.areNextLessonNotificationsEnabled()) {
            //The notifications are disabled: nothing to do!
            stopSelf();
            return START_NOT_STICKY;
        }

        //How this should work:
        //1) Always show notifications of the next lessons
        //2) Keep the notification shown until 15 minutes before the start of the next lesson. After
        //   that period show the notification for the next lesson.
        //3) If you've just finished the last, show until the end of the lesson +15 min that
        //   there are no more lessons today.
        //4) Reschedule the service to start 15 min before the start of the next lesson
        Networker.loadTodaysLessons(new TodaysLessonsListener(){

            @Override
            public void onLessonsAvailable(ArrayList<LessonSchedule> lessons) {
                //Finding passed lessons to hide the notifications of the past lessons
                ArrayList<LessonSchedule> passedLessons  = findPassedLessons(lessons);
                ArrayList<LessonSchedule> currentLessons = findCurrentLessons(lessons);

                hideNotificationsForPassedLessons(passedLessons);

                //Calculating lessons that will possibly be shown
                ArrayList<LessonSchedule> validLessons = getValidLessons(lessons);
                validLessons.removeAll(passedLessons);

                if (validLessons.isEmpty()) {
                    //We have no (more) lessons today. We'll schedule for tomorrow
                    showToastIfInDebug(NextLessonNotificationService.this, "No lessons for today!");
                    scheduleAtNextDayMorning();
                } else {
                    //Finding the lessons starting in less than N minutes:
                    ArrayList<LessonSchedule> lessonsStartingSoon = findLessonsStartingSoon(validLessons);
                    if (lessonsStartingSoon.isEmpty()) {
                        //TODO: fetch rooms if not available!
                        //There are no lessons starting soon, however we still have some lessons to
                        //show. We're going to show the notification for the lessons starting next:
                        ArrayList<LessonSchedule> nextLessons = findLessonsStartingNext(validLessons);
                        showNotificationsForLessons(nextLessons);

                        //Since we've already shown notifications for these lessons, we won't
                        //consider them for the next scheduling:
                        validLessons.removeAll(nextLessons);
                        scheduleForTheNextLessonAndStop(validLessons, currentLessons);
                    } else {
                        //We have to show a notification for each lesson:
                        showNotificationsForLessons(lessonsStartingSoon);

                        //We have already shown notifications for the lessons starting soon. We don't
                        //consider these for the next start calculation
                        validLessons.removeAll(lessonsStartingSoon);
                        scheduleForTheNextLessonAndStop(validLessons, currentLessons);
                    }
                }
            }

            @Override
            public void onLessonsCouldNotBeLoaded() {
                //We're going to wait for the lessons to be available; the service will still start
                //at the app opening and network state change.
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    private ArrayList<LessonSchedule> findCurrentLessons(ArrayList<LessonSchedule> lessons) {
        long now = CalendarUtils.getDebuggableMillis();

        ArrayList<LessonSchedule> currentLessons = new ArrayList<>();
        for (LessonSchedule lesson : lessons) {
            if(lesson.isHeldInMilliseconds(now)){
                currentLessons.add(lesson);
            }
        }
        return currentLessons;
    }

    /**
     * Finds all the next lessons starting at the same time
     */
    private ArrayList<LessonSchedule> findLessonsStartingNext(ArrayList<LessonSchedule> lessons) {
        ArrayList<LessonSchedule> lessonsStartingNext = new ArrayList<>();

        //Here we suppose that the ordering of the lessons didn't change:
        long nextTimeLessonStating = lessons.get(0).getStartsAt();
        for (LessonSchedule lesson : lessons) {
            if (lesson.getStartsAt() == nextTimeLessonStating) {
                lessonsStartingNext.add(lesson);
            }
        }

        return lessonsStartingNext;
    }

    private void showNotificationsForLessons(ArrayList<LessonSchedule> lessonsStartingSoon) {
        for (LessonSchedule lesson : lessonsStartingSoon) {
            showStartingNotificationForLessons(lesson);
        }
    }

    private void hideNotificationsForPassedLessons(ArrayList<LessonSchedule> passedLessons) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        for (LessonSchedule lesson : passedLessons) {
            manager.cancel((int) lesson.getId());
        }
    }

    private ArrayList<LessonSchedule> findPassedLessons(ArrayList<LessonSchedule> lessons) {
        ArrayList<LessonSchedule> passedLessons = new ArrayList<>();

        long now = CalendarUtils.getDebuggableMillis();
        for (LessonSchedule lesson : lessons) {
            if (lesson.getStartsAt() <= now) {
                passedLessons.add(lesson);
            }
        }
        return passedLessons;
    }

    @NonNull
    private ArrayList<LessonSchedule> findLessonsStartingSoon(ArrayList<LessonSchedule> lessons) {
        long millisSoon = CalendarUtils.getMillisWithMinutesDelta(Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN);

        ArrayList<LessonSchedule> lessonsStartingSoon = new ArrayList<>();
        for (LessonSchedule lesson: lessons) {
            if(lesson.startsBefore(millisSoon)){
                lessonsStartingSoon.add(lesson);
            }
        }
        return lessonsStartingSoon;
    }

    private void scheduleForTheNextLessonAndStop(ArrayList<LessonSchedule> lessons, ArrayList<LessonSchedule> currentLessons) {
        //Note: If we're finishing a lesson and don't have the next lesson starting immediately at
        // the end of it, then we must show a notification before the end of that lessons telling us
        // when the next lessons starts.

        //Finding when the next lessons start
        Long nextLessonStart = null;
        for (LessonSchedule lesson : lessons) {
            if (nextLessonStart == null || lesson.getStartsAt() < nextLessonStart) {
                nextLessonStart = lesson.getStartsAt();
            }
        }

        //Finding the end of the current lessons
        Long currentLessonEnd = null;
        for (LessonSchedule lesson : currentLessons) {
            if (currentLessonEnd == null || lesson.getEndsAt() < currentLessonEnd) {
                currentLessonEnd = lesson.getEndsAt();
            }
        }

        //Deciding when to plan the next schedule
        Long nextStartPlannedAt;
        if (nextLessonStart != null && currentLessonEnd != null) {
            nextStartPlannedAt = Math.min(nextLessonStart, currentLessonEnd);
        } else if (nextLessonStart == null && currentLessonEnd == null) {
            nextStartPlannedAt = null;
        } else {
            nextStartPlannedAt = nextLessonStart != null ? nextLessonStart : currentLessonEnd;
        }

        if (nextStartPlannedAt == null) {
            scheduleAtNextDayMorning();
        } else {
            scheduleNextStartAt(CalendarUtils.addMinutes(nextStartPlannedAt, -Config.NEXT_LESSON_NOTIFICATION_ANTICIPATION_MIN));
        }

        stopSelf();
    }

    private void scheduleAtNextDayMorning() {
        scheduleNextStartAt(getNextDayMorning());
    }

    private long getNextDayMorning() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, 1);

        return calendar.getTimeInMillis();
    }

    private void scheduleNextStartAt(long ms) {
        Intent serviceIntent = new Intent(this, NextLessonNotificationService.class);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, ms, pendingIntent);

        showToastIfInDebug(this, "Scheduled alarm manager to "+ CalendarUtils.formatTimestamp(ms));
    }

    private void showStartingNotificationForLessons(LessonSchedule lesson) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(lesson.getSubject())
                    .setContentText(lesson.getSynopsis())
                    .setColor(getResources().getColor(R.color.colorNotification))
                    .setAutoCancel(true);

        if (AppPreferences.areNextLessonNotificationsFixed()) {
            notificationBuilder.setOngoing(true);
        }


        Intent intent = new Intent(this, FirstActivityChooserActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        notificationBuilder.setContentIntent(resultPendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) lesson.getId(), notificationBuilder.build());
    }

    private ArrayList<LessonSchedule> getValidLessons(ArrayList<LessonSchedule> lessons) {
        ArrayList<LessonSchedule> lessonsToKeep = new ArrayList<>();
        ArrayList<Long> typesToHide = AppPreferences.getLessonTypesIdsToHide();

        for (LessonSchedule lesson : lessons) {
            //Is it filtered by the lesson type or the lessons already passed?
            if (!typesToHide.contains(lesson.getLessonTypeId())) {
                //Checking if we're filtering this lessons because of partitionings
                if (!isFilteredByPartitionings(lesson)) {
                    lessonsToKeep.add(lesson);
                }
            }

        }

        return lessonsToKeep;
    }

    private boolean isFilteredByPartitionings(LessonSchedule lesson) {
        for (String partitioningText : AppPreferences.getHiddenPartitionings(lesson.getLessonTypeId())) {
            if (lesson.getFullDescription().contains("("+partitioningText+")")) {
                return true;
            }
        }
        return false;
    }

    public static Intent createIntent(Context context, int starter) {
        Intent intent = new Intent(context, NextLessonNotificationService.class);
        intent.putExtra(EXTRA_STARTER, starter);
        return intent;
    }

    public static void clearNotifications(Context context) {
        //Technically this could cancel the "Your lessons has changed" notification. However, this
        //would happen very infrequently since the changes to lesson are not performed very often
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }
}
