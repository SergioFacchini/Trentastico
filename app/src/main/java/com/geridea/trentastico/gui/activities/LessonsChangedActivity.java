package com.geridea.trentastico.gui.activities;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.adapters.DiffResultAdapter;
import com.geridea.trentastico.network.request.LessonsDiffResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LessonsChangedActivity extends AppCompatActivity {

    public static final String EXTRA_DIFF_RESULT = "EXTRA_DIFF_RESULT";

    @BindView(R.id.lessons_view) ListView lessonsView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lessons_changed);
        ButterKnife.bind(this);

        //No need for action bar in the main activity
        getSupportActionBar().hide();

        LessonsDiffResult diffResult = (LessonsDiffResult) getIntent().getSerializableExtra(EXTRA_DIFF_RESULT);
        lessonsView.setAdapter(new DiffResultAdapter(this, diffResult));

    }

    @OnClick(R.id.close_button)
    void onCloseButtonPressed() {
        finish();
    }

    @OnClick(R.id.show_lessons_button)
    void onShowLessonsButtonPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}
