package com.geridea.trentastico.gui.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.views.CourseSelectorView
import com.geridea.trentastico.utils.AppPreferences

class WelcomeActivity : AppCompatActivity() {

    @BindView(R.id.course_selector) lateinit var courseSelector: CourseSelectorView
    @BindView(R.id.start_button)    lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        //No need for action bar in the main activity
        supportActionBar!!.hide()

        ButterKnife.bind(this)

        startButton.visibility = View.GONE

        courseSelector.onCoursesLoaded.connect { startButton.visibility = View.VISIBLE }
        courseSelector.loadCourses()
    }

    @OnClick(R.id.start_button)
    fun onStartButtonPressed() {
        val selectedStudyCourse = courseSelector.buildStudyCourse()
        if (selectedStudyCourse != null) {
            AppPreferences.studyCourse = selectedStudyCourse
            AppPreferences.isFirstRun = false

            startActivity(Intent(this, HomeActivity::class.java))

            finish()
        }
    }

}
