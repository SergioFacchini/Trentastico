package com.geridea.trentastico.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.views.CourseSelectorView;
import com.geridea.trentastico.utils.AppPreferences;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class WelcomeActivity extends AppCompatActivity {

    @BindView(R.id.course_selector)
    CourseSelectorView courseSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //No need for action bar in the main activity
        getSupportActionBar().hide();

        ButterKnife.bind(this);
    }

    @OnClick(R.id.start_button)
    public void onStartButtonPressed(View view) {
        AppPreferences.setStudyCourse(courseSelector.getSelectedStudyCourse());
        AppPreferences.setIsFirstRun(false);

        startActivity(new Intent(this, HomeActivity.class));

        finish();
    }

}
