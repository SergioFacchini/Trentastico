package com.geridea.trentastico.receivers;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.geridea.trentastico.services.LessonsUpdaterService;
import com.geridea.trentastico.utils.ContextUtils;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Starting the lessons monitoring service at boot
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            startService(context, LessonsUpdaterService.STARTER_BOOT_BROADCAST);
        } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
            //We just switched internet on (or off, or changed from mobile to WIFI)
            if(ContextUtils.weHaveInternet(context)){
                startService(context, LessonsUpdaterService.STARTER_NETWORK_BROADCAST);
            }
        }
    }

    private ComponentName startService(Context context, int starter) {
        return context.startService(LessonsUpdaterService.createServiceIntent(context, starter));
    }

}
