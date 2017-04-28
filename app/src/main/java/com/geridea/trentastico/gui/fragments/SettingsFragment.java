
package com.geridea.trentastico.gui.fragments;

/*
 * Created with ♥ by Slava on 19/03/2017.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems;
import com.geridea.trentastico.gui.views.CourseSelectorView;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.services.LessonsUpdaterService;
import com.geridea.trentastico.services.NLNStarter;
import com.geridea.trentastico.services.NextLessonNotificationService;
import com.geridea.trentastico.utils.AppPreferences;
import com.threerings.signals.Listener1;
import com.threerings.signals.Signal1;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

public class SettingsFragment extends FragmentWithMenuItems {

    public static final int MIN_CALENDAR_FONT_SIZE = 7;
    /**
     * Prevents listeners from triggering unnecessarily.
     */
    boolean isLoading = true;

    //Calendar
    @BindView(R.id.font_size_seek_bar) SeekBar fontSizeSeekBar;
    @BindView(R.id.font_preview) TextView fontSizePreview;

    //Study course
    @BindView(R.id.current_study_course) TextView currentStudyCourse;

    //Lessons updates
    @BindView(R.id.search_for_lesson_changes) Switch searchForLessonChanges;
    @BindView(R.id.lesson_change_show_notification) Switch shownNotificationOnLessonChanges;

    //Next lesson notification
    @BindView(R.id.show_next_lesson_notification) Switch showNextLessonNotification;
    @BindView(R.id.make_notifications_fixed) Switch makeNotificationFixedSwitch;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isLoading = true;

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, view);

        //Calendar
        fontSizeSeekBar.setProgress(AppPreferences.getCalendarFontSize() - MIN_CALENDAR_FONT_SIZE);
        updateCalendarFontPreview(AppPreferences.getCalendarFontSize());

        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCalendarFontPreview(MIN_CALENDAR_FONT_SIZE + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                AppPreferences.setCalendarFontSize(seekBar.getProgress() + MIN_CALENDAR_FONT_SIZE);
            }
        });

        //Study courses
        StudyCourse studyCourse = AppPreferences.getStudyCourse();
        currentStudyCourse.setText(studyCourse.generateFullDescription());

        //Lesson changes
        searchForLessonChanges.setChecked(AppPreferences.isSearchForLessonChangesEnabled());
        shownNotificationOnLessonChanges.setChecked(AppPreferences.isNotificationForLessonChangesEnabled());

        //Next lesson notification
        showNextLessonNotification.setChecked(AppPreferences.areNextLessonNotificationsEnabled());
        makeNotificationFixedSwitch.setChecked(AppPreferences.areNextLessonNotificationsFixed());

        isLoading = false;

        return view;
    }

    private void updateCalendarFontPreview(int fontSize) {
        fontSizePreview.setTextSize(COMPLEX_UNIT_SP, fontSize);
    }

    ///////////////////////////
    ////LESSONS UPDATES
    ///////////////////////////

    @OnClick(R.id.change_study_course_button)
    void onChangeStudyCourseButtonPressed(){
        ChangeStudyCourseDialog dialog = new ChangeStudyCourseDialog(getActivity());
        dialog.onChoiceMade.connect(new Listener1<StudyCourse>() {
            @Override
            public void apply(StudyCourse studyCourse) {
                currentStudyCourse.setText(studyCourse.generateFullDescription());
            }
        });
        dialog.show();
    }

    @OnCheckedChanged(R.id.search_for_lesson_changes)
    void onSearchForLessonsSwitchChanged(boolean enabled){
        if (isLoading) {
            return;
        }

        AppPreferences.setSearchForLessonChangesEnabled(enabled);
        shownNotificationOnLessonChanges.setEnabled(enabled);

        if (enabled) {
            getActivity().startService(
                LessonsUpdaterService.createIntent(getActivity(), LessonsUpdaterService.STARTER_SETTING_CHANGED)
            );
        } else {
            LessonsUpdaterService.cancelSchedules(getActivity(), LessonsUpdaterService.STARTER_SETTING_CHANGED);
        }
    }

    @OnCheckedChanged(R.id.search_for_lesson_changes)
    void onShowLessonChangeNotificationSwitchChanged(boolean checked){
        if (isLoading) {
            return;
        }

        AppPreferences.setNotificationForLessonChangesEnabled(checked);
    }

    ///////////////////////////
    ////NEXT LESSON NOTIFICATION
    ///////////////////////////

    @OnCheckedChanged(R.id.show_next_lesson_notification)
    void onShowNextLessonNotificationSwitchChanged(boolean checked){
        if (isLoading) {
            return;
        }

        AppPreferences.setNextLessonNotificationsEnabled(checked);
        makeNotificationFixedSwitch.setEnabled(checked);

        if (checked) {
            startNextLessonNotificationService();
        } else {
            NextLessonNotificationService.clearNotifications(getActivity());
        }
    }

    private void startNextLessonNotificationService() {
        getActivity().startService(NextLessonNotificationService.createIntent(
            getActivity(), NLNStarter.NOTIFICATIONS_SWITCHED_ON)
        );
    }

    @OnCheckedChanged(R.id.make_notifications_fixed)
    void onMakeNotificationsFixedSwitchChanged(boolean checked){
        if (isLoading) {
            return;
        }

        AppPreferences.setNextLessonNotificationsFixed(checked);

        //If we have any notification, we have to update them:
        startNextLessonNotificationService();
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[0];
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        //Does not uses menus, nothing to bind!
    }


    protected class ChangeStudyCourseDialog extends AlertDialog {

        /**
         * Dispatched when the user changed or did not change the study course and just pressed OK.
         */
        public final Signal1<StudyCourse> onChoiceMade = new Signal1<>();

        @BindView(R.id.course_selector) CourseSelectorView courseSelector;

        protected ChangeStudyCourseDialog(@NonNull Context context) {
            super(context);

            final View view = Views.inflate(context, R.layout.dialog_change_study_course);
            ButterKnife.bind(this, view);

            courseSelector.setStudyCourse(AppPreferences.getStudyCourse());

            setView(view);
        }

        @OnClick(R.id.change_button)
        void onChangeStudyCourseButtonClicked(){
            StudyCourse selectedCourse = courseSelector.getSelectedStudyCourse();
            if (AppPreferences.getStudyCourse().equals(selectedCourse)) {
                //We just clicked ok without changing our course...
                onChoiceMade.dispatch(selectedCourse);
            } else {
                clearFilters();
                clearCache(selectedCourse);
                removeOverlappingExtraCourses(selectedCourse);

                AppPreferences.setStudyCourse(selectedCourse);

                dealWithNextLessonNotifications();
                onChoiceMade.dispatch(selectedCourse);
            }

            dismiss();
        }

        private void dealWithNextLessonNotifications() {
            NextLessonNotificationService.clearNotifications(getContext());

            //We need to show the next lesson notification for the new course
            getActivity().startService(NextLessonNotificationService.createIntent(
                    getContext(), NLNStarter.STUDY_COURSE_CHANGE
            ));
        }

        private void removeOverlappingExtraCourses(StudyCourse selectedCourse) {
            AppPreferences.removeExtraCoursesHaving(selectedCourse.getCourseId(), selectedCourse.getYear());
        }

        private void clearFilters() {
            AppPreferences.removeAllHiddenCourses(); //No longer need them
            AppPreferences.removeAllHiddenPartitionings(); //No longer need them
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

    }

}
