package com.geridea.trentastico.gui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.FrameLayout
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnItemSelected
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.adapters.CoursesAdapter
import com.geridea.trentastico.gui.adapters.YearsAdapter
import com.geridea.trentastico.gui.views.utils.LEPState
import com.geridea.trentastico.model.Course
import com.geridea.trentastico.model.StudyCourse
import com.geridea.trentastico.model.StudyYear
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.CoursesLoadingListener
import com.geridea.trentastico.utils.UIUtils.runOnMainThread
import com.threerings.signals.Signal0
import com.threerings.signals.Signal1
import kotlinx.android.synthetic.main.view_course_selector.view.*

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

class CourseSelectorView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    val onCoursesLoaded: Signal0 = Signal0()

    /**
     * Dispatched when the user has just selected another study course.
     */
    val onCourseChanged = Signal1<StudyCourse>()

    /**
     * The study course that is currently selected. Can be null if the loading of the courses
     * is still not completed.
     */
    private var studyCourseToSelect: StudyCourse? = null

    /**
     * Due to a strange way the setOnItemSelectedListener is dispatched on department, selecting a
     * course immediately after having selected the department will cause the
     * setOnItemSelectedListener to be dispatched later, so the selected course will be lost. This
     * variable is a workaround.
     */
    private var yearToSelect: String? = null

    fun selectStudyCourse(studyCourse: StudyCourse) {
        studyCourseToSelect = studyCourse
        yearToSelect        = studyCourse.yearId

        if (isLoadingCompleted) {
            selectCourseWithId(studyCourse.courseId)
            selectYearWithId(studyCourse.yearId)
        }
    }

    fun buildStudyCourse(): StudyCourse {
        val selectedCourse = coursesSpinner.selectedItem as Course
        val selectedYear = yearSpinner.selectedItem as StudyYear

        return StudyCourse(
                courseId = selectedCourse.id,
                courseName = selectedCourse.name,
                yearId = selectedYear.id,
                yearName = selectedYear.name
        )
    }

    var isLoadingCompleted = false
        private set

    init {
        Views.inflateAndAttach<View>(this, R.layout.view_course_selector)
        ButterKnife.bind(this, this)

        coursesSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCourse = coursesSpinner.selectedItem as Course

                val adapter = YearsAdapter(context, selectedCourse.studyYears)
                yearSpinner.adapter = adapter
                if(yearToSelect != null){
                    yearSpinner.setSelection(adapter.getPositionOfYearWithId(yearToSelect!!)?:0, false)
                }

                onCourseChanged.dispatch(buildStudyCourse())
            }
        }
    }

    @OnItemSelected(R.id.yearSpinner)
    internal fun onYearSelected() = onCourseChanged.dispatch(buildStudyCourse())

    @OnClick(R.id.retryButton)
    internal fun onRetryButtonClick() = loadCourses()

    fun loadCourses() {
        Networker.loadStudyCourses(object : CoursesLoadingListener {
            override fun onCoursesFetched(courses: List<Course>) {
                //Updating UI
                runOnMainThread {
                    isLoadingCompleted = true

                    coursesSpinner.adapter = CoursesAdapter(context, courses)
                    yearSpinner.adapter    = YearsAdapter(context, courses.first().studyYears)

                    if (studyCourseToSelect != null) {
                        //The selected study course has been set while the loading was still not
                        //performed. Once we finish loading, we have to choose the set study course
                        selectStudyCourse(studyCourseToSelect!!)
                    }

                    lepView.currentView = LEPState.PRESENT

                    onCoursesLoaded.dispatch()
                }
            }

            override fun onLoadingError() {
                showErrorMessage(
                        "Non sono riuscito a scaricare l'elenco dei corsi. Hai una " +
                                "connessione ad internet attiva?"
                )
            }

            override fun onParsingError(exception: Exception) {
                showErrorMessage(
                        "Si è verificato un errore nel recupero dei corsi. Provare a " +
                                "ricaricare. Se il problema persiste, riprovare dopo qualche ora " +
                                "(il sito degli orari potrebbe essere sotto manutenzione)"
                )
            }

        })
    }

    private fun selectCourseWithId(idToSearch: String) {
        //It might be possible that a specific course gets removed. In this situation the
        //app should not crash, but just ignore the selection
        val position = (coursesSpinner.adapter as CoursesAdapter).getPositionOfCourseWithId(idToSearch)
        if (position != null) {
            coursesSpinner.setSelection(position, false)
        }
    }

    private fun selectYearWithId(idToSearch: String) {
        //It might be possible that a specific year gets removed. In this situation the
        //app should not crash, but just ignore the selection
        val position = (yearSpinner.adapter as YearsAdapter).getPositionOfYearWithId(idToSearch)
        if (position != null) {
            coursesSpinner.setSelection(position, false)
        }
    }

    private fun showErrorMessage(errorMessage: String) = runOnMainThread {
        errorText.text = errorMessage

        lepView.currentView = LEPState.ERROR
    }

}
