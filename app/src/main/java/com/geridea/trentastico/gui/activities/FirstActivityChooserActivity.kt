package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.utils.AppPreferences

/**
 * Chooses the right activity to start the application with.
 */
class FirstActivityChooserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BugLogger.info("Application is launched", "APP")

        when {
            AppPreferences.isFirstRun
                -> startActivity(Intent(this, WelcomeActivity::class.java))
            AppPreferences.hasToUpdateStudyCourse
                -> startActivity(Intent(this, UpdateStudyCourseActivity::class.java))
            else
                -> startActivity(Intent(this, HomeActivity::class.java))
        }

        finish()
    }
}
