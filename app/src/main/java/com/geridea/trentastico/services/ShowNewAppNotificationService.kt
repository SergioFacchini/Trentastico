package com.geridea.trentastico.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.res.ResourcesCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.HomeActivity
import java.util.*
import java.util.concurrent.TimeUnit


/*
 * Created with ♥
 */
class ShowNewAppNotificationService: Job() {

    override fun onRunJob(params: Params): Result {
        showRoomTickNotification(context)

        return Result.SUCCESS
    }

    companion object {

        const val BUNDLE_SHOW_OTHER_APPS = "BUNDLE_SHOW_OTHER_APPS"

        const val TAG = "NEW_APP"

        private const val NOTIFICATION_OTHER_APPS_ID: Int = 1009

        private const val NOTIFICATION_CHANNEL_ID = "Nuove app"

        @RequiresApi(Build.VERSION_CODES.O)
        private fun buildNotificationChannel(notificationManager: NotificationManager) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, importance)
            channel.description = "Mostra una notifica riguardo a nuove app"

            notificationManager.createNotificationChannel(channel)
        }


        fun showRoomTickNotification(context: Context) {
            // Create the notification channel for android O
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                buildNotificationChannel(notificationManager)
            }


            //Creating intent opening the new apps screen
            val bundle = Bundle()
            bundle.putBoolean(BUNDLE_SHOW_OTHER_APPS, true)

            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtras(bundle)

            //Appending an intent to the notification
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val color = ResourcesCompat.getColor(context.resources, R.color.colorNotification, null)
            val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("RoomTick: Coinquilini e Studenti Fuori Sede")
                    .setContentText("Una nuova app dal creatore di Trentastico")
                    .setColor(color)
                    .setAutoCancel(true)
                    .setContentIntent(pending)

            notificationManager.notify(NOTIFICATION_OTHER_APPS_ID, notificationBuilder.build())
        }

        fun showNewAppNotificationIfNeeded() {
            val releaseDay = Calendar.getInstance()
            releaseDay.set(2020, Calendar.JANUARY, 22, 19, 0)

            // Job request treats execution window as offset from now
            val fromMillis = releaseDay.timeInMillis - System.currentTimeMillis()
            if (fromMillis > 0) { //Don't show the notification in the past
                val toMillis = fromMillis + TimeUnit.HOURS.toMillis(1)

                JobRequest.Builder(TAG)
                        .setExecutionWindow(fromMillis, toMillis)
                        .setUpdateCurrent(true)
                        .build()
                        .schedule()
            }

        }


    }


}