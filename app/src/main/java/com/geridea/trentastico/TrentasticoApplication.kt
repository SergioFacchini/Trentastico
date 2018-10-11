package com.geridea.trentastico

import android.app.Application
import com.alexvasilkov.android.commons.utils.AppContext
import com.amitshekhar.DebugDB
import com.evernote.android.job.JobManager
import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.LessonsUpdaterJob
import com.geridea.trentastico.services.ServicesJobCreator
import com.geridea.trentastico.utils.*


/*
 * Created with ♥ by Slava on 03/03/2017.
 */

//TODO: reenable ACRA
//@ReportsCrashes(formUri = "http://collector.tracepot.com/20579ea2", mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast_text)
class TrentasticoApplication : Application() {

    val NOTIFICATION_LESSONS_CHANGED_ID = 1000


    override fun onCreate() {
        super.onCreate()

        AppPreferences.init(this)

        //Disabling db debug page if in debug mode
        if (!IS_IN_DEBUG_MODE) {
            DebugDB.shutDown()
        }

        //TODO: reenable ACRA
        //ACRA.init(this)

        AppContext.init(this)

        Networker.init(Cacher(this))

        ColorDispenser.init(this)

        BugLogger.init(this)
        BugLogger.onNewDebugMessageArrived.connect { message ->
            if(IS_IN_DEBUG_MODE){
                UIUtils.showToastIfInDebug(applicationContext, message)
            }
        }

        JobManager.create(this).addJobCreator(ServicesJobCreator())

        //"Services"
        LessonsUpdaterJob.onLessonsChanged.connect { diffResult, courseName ->
            LessonsUpdaterJob.showLessonsChangedNotification(applicationContext, diffResult, courseName)
        }


        //Leave this last since it might have some other dependencies of other singletons
        VersionManager.checkForVersionChangeCode()
    }

}
