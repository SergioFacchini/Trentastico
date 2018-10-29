package com.geridea.trentastico.services

import android.content.Context
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ContextUtils
import com.geridea.trentastico.utils.time.CalendarUtils

class DonationPopupManager {

    companion object {

        fun install() {
            val nextSchedule = CalendarUtils.addMinutes(System.currentTimeMillis(), 60)
            AppPreferences.nextDonationNotificationDate = nextSchedule
        }

        fun shouldPopupBeShown(context: Context): Boolean =
                AppPreferences.showDonationPopups &&
                AppPreferences.nextDonationNotificationDate <= System.currentTimeMillis() &&
                        ContextUtils.weHaveInternet(context)

        fun rescheduleNotification() {
            val nextSchedule = CalendarUtils.addDays(System.currentTimeMillis(), 5)
            AppPreferences.nextDonationNotificationDate = nextSchedule
        }

    }

}
