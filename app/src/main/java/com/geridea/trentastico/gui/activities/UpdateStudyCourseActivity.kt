package com.geridea.trentastico.gui.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.geridea.trentastico.Config
import com.geridea.trentastico.R
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import kotlinx.android.synthetic.main.activity_update_your_course.*

class UpdateStudyCourseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_your_course)

        //No need for action bar in the main activity
        supportActionBar!!.hide()

        startButton.visibility = View.GONE
        startButton.setOnClickListener {
            val selectedStudyCourse = courseSelector.buildStudyCourse()
            AppPreferences.studyCourse = selectedStudyCourse

            AppPreferences.currentStudyYear = Config.CURRENT_STUDY_YEAR.toInt()
            AppPreferences.hasToUpdateStudyCourse = false

            //Showing next lesson notification
            NextLessonNotificationService.scheduleNowIfEnabled()

            startActivity(Intent(this, HomeActivity::class.java))

            finish()
        }


        courseSelector.onCoursesLoaded.connect { startButton.visibility = View.VISIBLE }
        courseSelector.loadCourses()

    }


}
