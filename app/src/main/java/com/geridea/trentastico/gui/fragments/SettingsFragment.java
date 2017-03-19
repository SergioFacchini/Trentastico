
package com.geridea.trentastico.gui.fragments;

/*
 * Created with ♥ by Slava on 19/03/2017.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.geridea.trentastico.R;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.gui.views.CourseSelectorView;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.utils.AppPreferences;
import com.threerings.signals.Signal0;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsFragment extends Fragment {

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
            //We changed our course, let's wipe out all the cache!
            Cacher.purge();
            AppPreferences.setStudyCourse(selectedCourse);

            onChoiceMade.dispatch();
        }
    }

}
