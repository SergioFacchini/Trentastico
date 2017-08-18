package com.geridea.trentastico.gui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.Spinner
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnItemSelected
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.adapters.CoursesAdapter
import com.geridea.trentastico.gui.adapters.DepartmentsAdapter
import com.geridea.trentastico.gui.adapters.YearsAdapter
import com.geridea.trentastico.model.Department
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.providers.DepartmentsProvider
import com.threerings.signals.Signal1

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

class CourseSelectorView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    @BindView(R.id.departements_spinner) lateinit var departmentsSpinner: Spinner
    @BindView(R.id.courses_spinner)      lateinit var coursesSpinner: Spinner
    @BindView(R.id.year_spinner)         lateinit var yearsSpinner: Spinner

    /**
     * Due to a strange way the setOnItemSelectedListener is dispatched on department, selecting a
     * course immediately after having selected the department will cause the
     * setOnItemSelectedListener to be dispatched later, so the selected course will be lost. This
     * variable is a workaround.
     */
    private var courseToSelectPosition = 0

    /**
     * Dispatched when the user has just selected another study course.
     */
    val onCourseChanged = Signal1<StudyCourse>()


    init {

        Views.inflateAndAttach<View>(this, R.layout.view_course_selector)
        ButterKnife.bind(this, this)

        departmentsSpinner.adapter = DepartmentsAdapter(context)

        val selectedDepartment = departmentsSpinner.selectedItem as Department
        coursesSpinner.adapter = CoursesAdapter(context, selectedDepartment)

        yearsSpinner.adapter = YearsAdapter(context)
    }

    @OnItemSelected(R.id.departements_spinner)
    internal fun onDepartmentSelected(selectedPosition: Int) {
        val selectedDepartment = departmentsSpinner.getItemAtPosition(selectedPosition) as Department
        coursesSpinner.adapter = CoursesAdapter(context, selectedDepartment)
        coursesSpinner.setSelection(courseToSelectPosition, false)

        courseToSelectPosition = 0

        onCourseChanged.dispatch(selectedStudyCourse)
    }


    @OnItemSelected(R.id.courses_spinner)
    internal fun onCoursesSelected() = onCourseChanged.dispatch(selectedStudyCourse)

    @OnItemSelected(R.id.year_spinner)
    internal fun onYearSelected() = onCourseChanged.dispatch(selectedStudyCourse)

    val selectedStudyCourse: StudyCourse
        get() = StudyCourse(
                departmentsSpinner.selectedItemId,
                coursesSpinner.selectedItemId,
                yearsSpinner.selectedItemPosition + 1
        )

    fun setStudyCourse(newStudyCourse: StudyCourse) {
        val department = DepartmentsProvider.getDepartmentWithId(newStudyCourse.departmentId)
        courseToSelectPosition = department.getCoursePosition(newStudyCourse.courseId)

        val depPosition = DepartmentsProvider.getDepartmentPosition(newStudyCourse.departmentId)
        departmentsSpinner.setSelection(depPosition, false)


        yearsSpinner.setSelection(newStudyCourse.year - 1)
    }

}
