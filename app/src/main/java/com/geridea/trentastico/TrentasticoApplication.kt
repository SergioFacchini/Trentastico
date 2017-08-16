package com.geridea.trentastico

import android.app.Application

import com.alexvasilkov.android.commons.utils.AppContext
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.providers.DepartmentsProvider
import com.geridea.trentastico.utils.AppPreferences

import org.acra.ACRA
import org.acra.ReportingInteractionMode
import org.acra.annotation.ReportsCrashes

/*
 * Created with ♥ by Slava on 03/03/2017.
 */

@ReportsCrashes(formUri = "http://collector.tracepot.com/20579ea2", mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast_text)
class TrentasticoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (!Config.DEBUG_MODE) {
            ACRA.init(this)
        }

        AppContext.init(this)

        Networker.init(Cacher(this))

        AppPreferences.init(this)
        DepartmentsProvider.loadIfNeeded()

        BugLogger.init()

    }

}
