package trentastico.geridea.com.trentastico.activities.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.alexvasilkov.android.commons.utils.Views;

import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.views.CourseSelectorView;
import trentastico.geridea.com.trentastico.activities.utils.AppPreferences;

public class WelcomeActivity extends AppCompatActivity {

    private CourseSelectorView courseSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //No need for action bar in the main activity
        getSupportActionBar().hide();

        courseSelector = Views.find(this, R.id.course_selector);


        Views.find(this, R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppPreferences.setStudyCourse(courseSelector.getSelectedStudyCourse());
                AppPreferences.setIsFirstRun(false);

                startActivity(new Intent(WelcomeActivity.this, CalendarActivity.class));
            }
        });
    }

}
