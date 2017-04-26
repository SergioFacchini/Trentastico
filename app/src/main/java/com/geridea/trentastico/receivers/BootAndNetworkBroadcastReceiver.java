package com.geridea.trentastico.receivers;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.geridea.trentastico.services.LessonsUpdaterService;
import com.geridea.trentastico.services.NextLessonNotificationService;
import com.geridea.trentastico.utils.ContextUtils;

public class BootAndNetworkBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Starting the lessons monitoring service and show next lesson notifications at boot
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            startLessonUpdaterService(context, LessonsUpdaterService.STARTER_BOOT_BROADCAST);
            startNextLessonNotificationService(context, NextLessonNotificationService.STARTER_PHONE_BOOT);
        } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){

            //We just switched internet on (or off, or changed from mobile to WIFI)
            if(ContextUtils.weHaveInternet(context)){
                startLessonUpdaterService(context, LessonsUpdaterService.STARTER_NETWORK_BROADCAST);
                startNextLessonNotificationService(context, NextLessonNotificationService.STARTER_NETWORK_ON);
            }
        }
    }

    private void startNextLessonNotificationService(Context context, int starter) {
        context.startService(NextLessonNotificationService.createIntent(context, starter));
    }

    private void startLessonUpdaterService(Context context, int starter) {
        context.startService(LessonsUpdaterService.createIntent(context, starter));
    }

}
