package trentastico.geridea.com.trentastico.activities.gui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.alexvasilkov.android.commons.utils.Views;

import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.adapters.CoursesAdapter;
import trentastico.geridea.com.trentastico.activities.gui.adapters.DepartmentsAdapter;
import trentastico.geridea.com.trentastico.activities.gui.adapters.YearsAdapter;
import trentastico.geridea.com.trentastico.activities.model.Department;
import trentastico.geridea.com.trentastico.activities.model.StudyCourse;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

public class CourseSelectorView extends FrameLayout {

    private final Spinner departmentsSpinner;
    private final Spinner coursesSpinner;
    private final Spinner yearsSpinner;

    public CourseSelectorView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        Views.inflateAndAttach(this, R.layout.view_course_selector);

        departmentsSpinner = Views.find(this, R.id.departements_spinner);
        departmentsSpinner.setAdapter(new DepartmentsAdapter(context));
        departmentsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                Department selectedDepartment = (Department) adapterView.getItemAtPosition(pos);
                coursesSpinner.setAdapter(new CoursesAdapter(context, selectedDepartment));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        coursesSpinner = Views.find(this, R.id.courses_spinner);
        coursesSpinner.setAdapter(new CoursesAdapter(context, (Department) departmentsSpinner.getSelectedItem()));

        yearsSpinner = Views.find(this, R.id.year_spinner);
        yearsSpinner.setAdapter(new YearsAdapter(context));
    }


    public StudyCourse getSelectedStudyCourse(){
        return new StudyCourse(
                departmentsSpinner.getSelectedItemId(),
                coursesSpinner    .getSelectedItemId(),
                yearsSpinner      .getSelectedItemId()
        );
    }

}
