package com.geridea.trentastico.receivers;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.geridea.trentastico.services.LessonsUpdatesCheckerService;
import com.geridea.trentastico.utils.ContextUtils;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Starting the lessons monitoring service at boot
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            startUpdaterService(context);
        } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
            //We just switched internet on (or off, or changed from mobile to WIFI)
            if(ContextUtils.weHaveInternet(context)){
                startUpdaterService(context);
            }
        }
    }

    private void startUpdaterService(Context context) {
        context.startService(new Intent(context, LessonsUpdatesCheckerService.class));
    }

}
