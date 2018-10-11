package com.geridea.trentastico.gui.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.geridea.trentastico.R
import com.geridea.trentastico.services.LessonsUpdaterJob
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        //No need for action bar in the main activity
        supportActionBar!!.hide()

        startButton.visibility = View.GONE
        startButton.setOnClickListener {
            //Saving course
            val selectedStudyCourse = courseSelector.buildStudyCourse()
            AppPreferences.studyCourse = selectedStudyCourse
            AppPreferences.isFirstRun = false

            //Enabling updates
            LessonsUpdaterJob.schedulePeriodicRun()

            //Showing next lesson notification
            NextLessonNotificationService.scheduleNow()

            //Starting the home activity
            startActivity(Intent(this, HomeActivity::class.java))

            finish()
        }

        courseSelector.onCoursesLoaded.connect { startButton.visibility = View.VISIBLE }
        courseSelector.loadCourses()
    }


}
