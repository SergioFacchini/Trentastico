
package com.geridea.trentastico.gui.fragments;

/*
 * Created with ♥ by Slava on 19/03/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.geridea.trentastico.R;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.gui.views.CourseSelectorView;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.utils.AppPreferences;
import com.threerings.signals.Signal0;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsFragment extends IFragmentWithMenuItems {

    /**
     * Dispatched when the user changed or did not change the study course and just pressed OK.
     */
    public final Signal0 onChoiceMade = new Signal0();

    @BindView(R.id.course_selector) CourseSelectorView courseSelector;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);

        courseSelector.setStudyCourse(AppPreferences.getStudyCourse());

        return view;
    }

    @OnClick(R.id.change_button)
    void onChangeStudyCourseButtonClicked(){
        StudyCourse selectedCourse = courseSelector.getSelectedStudyCourse();
        if (AppPreferences.getStudyCourse().equals(selectedCourse)) {
            //We just clicked ok without changing our course...
            onChoiceMade.dispatch();
        } else {

            AppPreferences.removeAllHiddenCourses(); //No longer need them
            AppPreferences.removeAllHiddenPartitionings(); //No longer need them
            clearCache(selectedCourse);
            AppPreferences.removeExtraCoursesHaving(selectedCourse.getCourseId(), selectedCourse.getYear());

            AppPreferences.setStudyCourse(selectedCourse);

            onChoiceMade.dispatch();
        }
    }

    private void clearCache(StudyCourse selectedCourse) {
        //We changed our course, let's wipe out all the cache!
        Cacher.purgeStudyCourseCache();

        //If we've just selected a course that we already had in our extra course, we need to
        //delete that course from cache
        ArrayList<ExtraCourse> overlappingExtraCourses = AppPreferences.getExtraCoursesHaving(
                selectedCourse.getCourseId(), selectedCourse.getYear()
        );

        for (ExtraCourse overlappingExtraCourse : overlappingExtraCourses) {
            Cacher.removeExtraCoursesWithLessonType(overlappingExtraCourse.getLessonTypeId());
        }
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[0];
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        //Does not uses menus, nothing to bind!
    }
}
