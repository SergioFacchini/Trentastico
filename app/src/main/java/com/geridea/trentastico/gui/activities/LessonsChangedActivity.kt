package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.adapters.DiffResultAdapter
import com.geridea.trentastico.network.request.LessonsDiffResult
import kotlinx.android.synthetic.main.activity_lessons_changed.*

class LessonsChangedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lessons_changed)

        //No need for action bar in the main activity
        supportActionBar!!.hide()

        val diffResult = intent.getSerializableExtra(EXTRA_DIFF_RESULT) as LessonsDiffResult
        lessonsView.adapter = DiffResultAdapter(this, diffResult)

        close_button.setOnClickListener {
            finish()
        }

        show_lessons_button.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_DIFF_RESULT = "EXTRA_DIFF_RESULT"
    }

}
