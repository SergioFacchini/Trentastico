package com.geridea.trentastico.receivers


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

import com.geridea.trentastico.services.LessonsUpdaterService
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.ContextUtils

class BootAndNetworkBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //Starting the lessons monitoring service and show next lesson notifications at boot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            startLessonUpdaterService(context, LessonsUpdaterService.STARTER_BOOT_BROADCAST)
            startNextLessonNotificationService(context, NLNStarter.PHONE_BOOT)
        } else if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {

            //We just switched internet on (or off, or changed from mobile to WIFI)
            if (ContextUtils.weHaveInternet(context)) {
                startLessonUpdaterService(context, LessonsUpdaterService.STARTER_NETWORK_BROADCAST)
                startNextLessonNotificationService(context, NLNStarter.NETWORK_ON)
            }
        }
    }

    private fun startNextLessonNotificationService(context: Context, starter: NLNStarter) {
        context.startService(NextLessonNotificationService.createIntent(context, starter))
    }

    private fun startLessonUpdaterService(context: Context, starter: Int) {
        context.startService(LessonsUpdaterService.createIntent(context, starter))
    }

}
