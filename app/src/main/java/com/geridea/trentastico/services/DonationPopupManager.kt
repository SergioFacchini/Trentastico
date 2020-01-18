package com.geridea.trentastico.services

import android.content.Context
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.ContextUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import java.util.*

class DonationPopupManager {

    companion object {

        fun install() {
            val nextSchedule = CalendarUtils.addMinutes(System.currentTimeMillis(), 60)
            AppPreferences.nextDonationNotificationDate = nextSchedule
        }

        fun shouldPopupBeShown(context: Context): Boolean {
            // Do not show the donation dialog dialog until the RoomTick notification arrived
            val roomTickReleaseDay = Calendar.getInstance()
            roomTickReleaseDay.set(2020, Calendar.JANUARY, 25, 23, 59)

            val roomTickReleaseDayMillis = roomTickReleaseDay.timeInMillis
            val now = System.currentTimeMillis()
            if (now <= roomTickReleaseDayMillis) {
                return false
            }

            return AppPreferences.showDonationPopups &&
                    AppPreferences.nextDonationNotificationDate <= System.currentTimeMillis() &&
                    ContextUtils.weHaveInternet(context)
        }

        fun rescheduleNotification() {
            val nextSchedule = CalendarUtils.addDays(System.currentTimeMillis(), 5)
            AppPreferences.nextDonationNotificationDate = nextSchedule
        }

    }

}
