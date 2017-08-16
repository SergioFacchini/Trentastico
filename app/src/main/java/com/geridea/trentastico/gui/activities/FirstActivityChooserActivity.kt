package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.geridea.trentastico.services.LessonsUpdaterService
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences

/**
 * Chooses the right activity to start the application with.
 */
class FirstActivityChooserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(LessonsUpdaterService.createIntent(this, LessonsUpdaterService.STARTER_APP_START))
        startService(NextLessonNotificationService.createIntent(this, NLNStarter.APP_BOOT))

        if (AppPreferences.isFirstRun) {
            startActivity(Intent(this, WelcomeActivity::class.java))
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        finish()
    }
}
