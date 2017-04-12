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

import com.geridea.trentastico.R;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.database.TodaysLessonsListener;
import com.geridea.trentastico.gui.activities.FirstActivityChooserActivity;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.CalendarUtils;

import java.util.ArrayList;
import java.util.Calendar;

import static com.geridea.trentastico.utils.UIUtils.showToastIfInDebug;

public class NextLessonNotificationService extends Service {

    public static final int NOTIFICATION_ANTICIPATION = 15;
    public static final int NOTIFICATION_ANTICIPATION_DELTA = 5;

    public static final int STARTER_APP_BOOT = 1;
    public static final int STARTER_DEBUG = 2;

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
            return START_NOT_STICKY;
        }

        //How this should work:
        //1) Always show notifications of the next lessons
        //2) Keep the notification shown until 15 minutes before the start of the next lesson. After
        //   that period show the notification for the next lesson.
        //3) If you've just finished the last, show until the end of the lesson +15 min that
        //   there are no more lessons today.
        //4) Reschedule the service to start 15 min before the start of the next lesson

        //Note:
        //If we show the notification 15 min before the start of each lesson, after the end of the
        //lunch we will not have shown where the next lesson is starting if we have a one hour gap
        //in our lessons.
        //if we show the notification at the end of each lesson, we might miss lessons that
        //are starting about 30 min after the start of that lesson.
        //The first situation is more simple to manage, so I will opt for that one.
        // There are however a couple of situations when i want
        // the notification to be shown even if there are currently no lessons starting in 15 min:
        // * at 7 o clock i want to show the user when it's first lesson is held. This is to make
        // him/her know when to go to university.
        // * 15 minutes before the end of the last lesson i want to display the user a message
        //telling that there are no other lessons planned for today.
        Cacher.getTodaysLessons(new TodaysLessonsListener(){

            @Override
            public void onLessonsAvailable(ArrayList<LessonSchedule> lessons) {
                ArrayList<LessonSchedule> filteredLessons = applyFiltersToLessons(lessons);

                if (lessons.isEmpty()) {
                    showToastIfInDebug(NextLessonNotificationService.this, "No lessons for today!");
                } else {
                    //Finding the lessons starting in less than 15 minutes:
                    ArrayList<LessonSchedule> lessonsStartingSoon = findLessonsStartingSoon(filteredLessons);
                    if (lessonsStartingSoon.isEmpty()) {
                        showToastIfInDebug(NextLessonNotificationService.this, "No lessons starting soon!");
                    } else {
                        //We have to show a notification for each lesson:
                        for (LessonSchedule lesson : filteredLessons) {
                            showStartingNotificationForLessons(lesson);
                        }
                    }
                    filteredLessons.removeAll(lessonsStartingSoon);
                    scheduleNextStart(filteredLessons);
                }
            }
        });

        return START_NOT_STICKY;
    }

    @NonNull
    private ArrayList<LessonSchedule> findLessonsStartingSoon(ArrayList<LessonSchedule> lessons) {
        Calendar cal15MinFromNow = Calendar.getInstance();
        cal15MinFromNow.add(Calendar.MINUTE, NOTIFICATION_ANTICIPATION);
        long millisSoon = cal15MinFromNow.getTimeInMillis();

        ArrayList<LessonSchedule> lessonsStartingSoon = new ArrayList<>();
        for (LessonSchedule lesson: lessons) {
            if(lesson.startsBefore(millisSoon)){
                lessonsStartingSoon.add(lesson);
            }
        }
        return lessonsStartingSoon;
    }

    private void scheduleNextStart(ArrayList<LessonSchedule> lessons) {
        Long nextLessonStart = null;
        for (LessonSchedule lesson : lessons) {
            if (nextLessonStart == null || lesson.getStartsAt() < nextLessonStart) {
                nextLessonStart = lesson.getStartsAt();
            }
        }

        if (nextLessonStart == null) {
            //We currently have no lesson at which start schedule the notification: we'll schedule
            //start tomorrow morning:
            scheduleNextStartAt(getNextDayMorning());
        } else {
            scheduleNextStartAt(nextLessonStart);
        }

        //TODO:
        // * if there are no other lessons today, make a db call to get the next one if any and
        //   schedule accordingly
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
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    .setAutoCancel(true);


        Intent intent = new Intent(this, FirstActivityChooserActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        notificationBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int) lesson.getId(), notificationBuilder.build());
    }

    private ArrayList<LessonSchedule> applyFiltersToLessons(ArrayList<LessonSchedule> lessons) {
        ArrayList<LessonSchedule> lessonsToKeep = new ArrayList<>();
        ArrayList<Long> typesToHide = AppPreferences.getLessonTypesIdsToHide();

        for (LessonSchedule lesson : lessons) {
            if (!typesToHide.contains(lesson.getLessonTypeId())) {

                //Checking if we're filtering this lessons because of partitionings
                ArrayList<String> partitioningsToFilter =
                        AppPreferences.getHiddenPartitionings(lesson.getLessonTypeId());

                //TODO: check that "partitioningText" includes the rounds ( )
                boolean isFilteredByPartitionings = false;
                for (String partitioningText : partitioningsToFilter) {
                    if (lesson.getFullDescription().contains(partitioningText)) {
                        isFilteredByPartitionings = true;
                        break;
                    }
                }

                if (!isFilteredByPartitionings) {
                    lessonsToKeep.add(lesson);
                }

            }

        }

        return lessonsToKeep;
    }

    public static Intent createIntent(Context context, int starter) {
        Intent intent = new Intent(context, NextLessonNotificationService.class);
        intent.putExtra(EXTRA_STARTER, starter);
        return intent;
    }

}
