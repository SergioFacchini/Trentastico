package trentastico.geridea.com.trentastico.activities.gui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.alexvasilkov.android.commons.utils.Views;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
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

    @BindView(R.id.departements_spinner) Spinner departmentsSpinner;
    @BindView(R.id.courses_spinner)      Spinner coursesSpinner;
    @BindView(R.id.year_spinner)         Spinner yearsSpinner;

    public CourseSelectorView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        Views.inflateAndAttach(this, R.layout.view_course_selector);
        ButterKnife.bind(this, this);

        departmentsSpinner.setAdapter(new DepartmentsAdapter(context));

        Department selectedDepartment = (Department) departmentsSpinner.getSelectedItem();
        coursesSpinner.setAdapter(new CoursesAdapter(context, selectedDepartment));

        yearsSpinner.setAdapter(new YearsAdapter(context));
    }

    @OnItemSelected(R.id.departements_spinner)
    void onDepartmentSelected(int selectedPosition){
        Department selectedDepartment = (Department) departmentsSpinner.getItemAtPosition(selectedPosition);
        coursesSpinner.setAdapter(new CoursesAdapter(getContext(), selectedDepartment));
    }

    public StudyCourse getSelectedStudyCourse(){
        return new StudyCourse(
                departmentsSpinner.getSelectedItemId(),
                coursesSpinner    .getSelectedItemId(),
                yearsSpinner      .getSelectedItemId()
        );
    }

}
