package com.geridea.trentastico.gui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.alexvasilkov.android.commons.utils.Views;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.adapters.CoursesAdapter;
import com.geridea.trentastico.gui.adapters.DepartmentsAdapter;
import com.geridea.trentastico.gui.adapters.YearsAdapter;
import com.geridea.trentastico.model.Department;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.providers.DepartmentsProvider;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

public class CourseSelectorView extends FrameLayout {

    @BindView(R.id.departements_spinner) Spinner departmentsSpinner;
    @BindView(R.id.courses_spinner)      Spinner coursesSpinner;
    @BindView(R.id.year_spinner)         Spinner yearsSpinner;

    /**
     * Due to a strange way the setOnItemSelectedListener is dispatched on department, selecting a
     * course immediately after having selected the department will cause the
     * setOnItemSelectedListener to be dispatched later, so the selected course will be lost. This
     * variable is a workaround.
     */
    private int courseToSelectPosition = 0;

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
        coursesSpinner.setSelection(courseToSelectPosition, false);

        courseToSelectPosition = 0;
    }

    public StudyCourse getSelectedStudyCourse(){
        return new StudyCourse(
                departmentsSpinner.getSelectedItemId(),
                coursesSpinner    .getSelectedItemId(),
                yearsSpinner      .getSelectedItemPosition()+1
        );
    }

    public void setStudyCourse(StudyCourse newStudyCourse){
        Department department = DepartmentsProvider.getDepartmentWithId(newStudyCourse.getDepartmentId());
        courseToSelectPosition = department.getCoursePosition(newStudyCourse.getCourseId());

        int depPosition = DepartmentsProvider.getDepartmentPosition(newStudyCourse.getDepartmentId());
        departmentsSpinner.setSelection(depPosition, false);


        yearsSpinner.setSelection(newStudyCourse.getYear()-1);
    }

}
