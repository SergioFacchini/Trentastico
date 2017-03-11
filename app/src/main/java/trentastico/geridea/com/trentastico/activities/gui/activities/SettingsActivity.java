package trentastico.geridea.com.trentastico.activities.gui.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;

import com.alexvasilkov.android.commons.utils.Views;

import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.adapters.DepartmentsAdapter;
import trentastico.geridea.com.trentastico.activities.gui.views.CourseSelectorView;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;

public class SettingsActivity extends AppCompatActivity {

    private CourseSelectorView courseSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_run);

        //No need for action bar in the main activity
        getSupportActionBar().hide();

        courseSelector = Views.find(this, R.id.course_selector);


        Views.find(this, R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StudyCourse selectedStudyCourse = courseSelector.getSelectedStudyCourse();

            }
        });
    }

}
