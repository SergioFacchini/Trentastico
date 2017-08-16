package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView

import com.geridea.trentastico.R
import com.geridea.trentastico.gui.adapters.DiffResultAdapter
import com.geridea.trentastico.network.request.LessonsDiffResult

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick

class LessonsChangedActivity : AppCompatActivity() {

    @BindView(R.id.lessons_view) internal var lessonsView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lessons_changed)
        ButterKnife.bind(this)

        //No need for action bar in the main activity
        supportActionBar!!.hide()

        val diffResult = intent.getSerializableExtra(EXTRA_DIFF_RESULT) as LessonsDiffResult
        lessonsView!!.adapter = DiffResultAdapter(this, diffResult)

    }

    @OnClick(R.id.close_button)
    internal fun onCloseButtonPressed() {
        finish()
    }

    @OnClick(R.id.show_lessons_button)
    internal fun onShowLessonsButtonPressed() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    companion object {

        val EXTRA_DIFF_RESULT = "EXTRA_DIFF_RESULT"
    }

}
