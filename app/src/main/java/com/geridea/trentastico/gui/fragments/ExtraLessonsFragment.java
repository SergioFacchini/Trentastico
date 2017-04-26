package com.geridea.trentastico.gui.fragments;


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems;
import com.geridea.trentastico.gui.adapters.ExtraCoursesAdapter;
import com.geridea.trentastico.gui.adapters.LessonTypesAdapter;
import com.geridea.trentastico.gui.views.CourseSelectorView;
import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.StudyCourse;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.network.request.listener.ListLessonsListener;
import com.geridea.trentastico.services.NLNStarter;
import com.geridea.trentastico.services.NextLessonNotificationService;
import com.geridea.trentastico.utils.AppPreferences;
import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;
import com.threerings.signals.Signal0;

import java.util.ArrayList;
import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;

import static android.view.View.GONE;

public class ExtraLessonsFragment extends FragmentWithMenuItems {

    @BindView(R.id.extra_lessons_list)     ListView lessonsList;
    @BindView(R.id.no_extra_courses_label) TextView noExtraCoursesLabel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_extra_lessons, container, false);
        ButterKnife.bind(this, view);

        initLessonsList();

        return view;
    }

    private void initLessonsList() {
        ArrayList<ExtraCourse> extraCourses = AppPreferences.getExtraCourses();
        if (extraCourses.isEmpty()) {
            noExtraCoursesLabel.setVisibility(View.VISIBLE);
        } else {
            noExtraCoursesLabel.setVisibility(View.GONE);
        }
        lessonsList.setAdapter(new ExtraCoursesAdapter(getActivity(), extraCourses));
    }

    @OnItemLongClick(R.id.extra_lessons_list)
    boolean onItemLongClick(int position){
        final ExtraCourse course = (ExtraCourse) lessonsList.getItemAtPosition(position);

        ExtraCourseDeleteDialog dialog = new ExtraCourseDeleteDialog(getActivity(), course);
        dialog.onDeleteConfirm.connect(new Listener0() {
            @Override
            public void apply() {
                initLessonsList();

                //Updating notifications
                NextLessonNotificationService.removeNotificationsOfExtraCourse(getContext(), course);
                NextLessonNotificationService.createIntent(
                        getActivity(), NLNStarter.EXTRA_COURSE_CHANGE
                );
            }
        });
        dialog.show();

        return true;
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[] { R.id.menu_add_extra_lessons };
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.menu_add_extra_lessons) {
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ExtraCourseAddDialog dialog = new ExtraCourseAddDialog(getActivity());
                    dialog.onNewCourseAdded.connect(new Listener0() {
                        @Override
                        public void apply() {
                            initLessonsList();

                            //Updating notifications
                            NextLessonNotificationService.createIntent(
                                getActivity(), NLNStarter.EXTRA_COURSE_CHANGE
                            );
                        }
                    });
                    dialog.show();
                    return true;
                }
            });
        }
    }

    class ExtraCourseDeleteDialog extends AlertDialog {

        private final ExtraCourse course;
        @BindView(R.id.course_name)       TextView courseName;
        @BindView(R.id.study_course_name) TextView studyCourseName;

        @BindView(R.id.color)             ImageView color;

        /**
         * Dispatched when the user has selected and added a new study course.
         */
        public final Signal0 onDeleteConfirm = new Signal0();

        public ExtraCourseDeleteDialog(Context context, ExtraCourse course) {
            super(context);
            this.course = course;

            View view = Views.inflate(context, R.layout.dialog_extra_course_delete);
            ButterKnife.bind(this, view);

            courseName.setText(course.getName());
            studyCourseName.setText(course.getStudyCourseFullName());
            color.setImageDrawable(new ColorDrawable(course.getColor()));

            setView(view);
        }

        @OnClick(R.id.cancel_button)
        void onCancelButtonPressed() {
            dismiss();
        }

        @OnClick(R.id.delete_button)
        void onDeleteButtonPressed() {
            AppPreferences.removeExtraCourse(course.getLessonTypeId());
            Cacher.removeExtraCoursesWithLessonType(course.getLessonTypeId());

            onDeleteConfirm.dispatch();
            dismiss();
        }

    }

    class ExtraCourseAddDialog extends AlertDialog {

        @BindView(R.id.cannot_select_current_study_course) TextView cannotSelectCurrentStudyCourse;
        @BindView(R.id.course_selector) CourseSelectorView courseSelector;
        @BindView(R.id.search_for_lessons) Button searchForLessonsButton;

        /**
         * Dispatched when the user has selected and added a new study course.
         */
        public final Signal0 onNewCourseAdded = new Signal0();

        protected ExtraCourseAddDialog(@NonNull Context context) {
            super(context);

            View view = Views.inflate(context, R.layout.dialog_extra_course_add);
            ButterKnife.bind(this, view);

            StudyCourse studyCourse = AppPreferences.getStudyCourse();
            studyCourse.decreaseOrChangeYear();

            courseSelector.setStudyCourse(studyCourse);
            courseSelector.onCourseChanged.connect(new Listener1<StudyCourse>() {
                @Override
                public void apply(StudyCourse newStudyCourse) {
                    if (newStudyCourse.equals(AppPreferences.getStudyCourse())) {
                        cannotSelectCurrentStudyCourse.setVisibility(View.VISIBLE);
                        searchForLessonsButton.setEnabled(false);
                    } else {
                        cannotSelectCurrentStudyCourse.setVisibility(GONE);
                        searchForLessonsButton.setEnabled(true);
                    }
                }
            });

            setView(view);
        }

        @OnClick(R.id.search_for_lessons)
        void onSearchForLessonsButtonClicked(){
            StudyCourse selectedStudyCourse = courseSelector.getSelectedStudyCourse();
            if (selectedStudyCourse.equals(AppPreferences.getStudyCourse())) {
                cannotSelectCurrentStudyCourse.setVisibility(View.VISIBLE);
                searchForLessonsButton.setEnabled(false);
                return;
            }

            ExtraCourseSearchDialog dialog = new ExtraCourseSearchDialog(
                    getActivity(), selectedStudyCourse
            );
            dialog.onCourseSelectedAndAdded.connect(new Listener0() {
                @Override
                public void apply() {
                    onNewCourseAdded.dispatch();
                    dismiss();
                }
            });
            dialog.show();
            dialog.searchForCourses();
        }

    }

    class ExtraCourseSearchDialog extends AlertDialog implements ListLessonsListener {

        private final StudyCourse studyCourse;
        @BindView(R.id.searching_lessons)     View searchingLessons;
        @BindView(R.id.lessons_found)         View lessonsFound;

        @BindView(R.id.selected_course)       TextView selectedCourseTextView;
        @BindView(R.id.error_while_searching) TextView errorWhileSearching;

        @BindView(R.id.lessons_list)          ListView lessonsList;

        /**
         * Dispatched when the user has selected a course and that course has been added to preferences.
         */
        public final Signal0 onCourseSelectedAndAdded = new Signal0();

        protected ExtraCourseSearchDialog(@NonNull Context context, StudyCourse studyCourse) {
            super(context);
            this.studyCourse = studyCourse;

            View view = Views.inflate(context, R.layout.dialog_extra_course_search);
            ButterKnife.bind(this, view);

            selectedCourseTextView.setText(
                    "Sto cercando le lezioni del corso da te selezionato: "+ studyCourse.generateFullDescription()
            );

            lessonsFound.setVisibility(GONE);

            setView(view);
        }

        public void searchForCourses() {
            Networker.loadCoursesOfStudyCourse(studyCourse, this);
        }

        @OnClick(R.id.cancel_search)
        void onCancelSearchButtonPressed() {
            dismiss();
        }

        @OnItemClick(R.id.lessons_list)
        void onLessonSelected(int position){
            LessonType lesson = (LessonType) lessonsList.getItemAtPosition(position);

            if (!AppPreferences.hasExtraCourseWithId(lesson.getId())) {
                AppPreferences.addExtraCourse(new ExtraCourse(studyCourse, lesson));

                dismiss();
                onCourseSelectedAndAdded.dispatch();
            }
        }

        @Override
        public void onErrorHappened(Exception error) {
            showErrorMessage();
        }

        private void showErrorMessage() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    errorWhileSearching.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onParsingErrorHappened(Exception e) {
            showErrorMessage();
        }

        @Override
        public void onLessonTypesRetrieved(final Collection<LessonType> lessonTypes) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lessonsList.setAdapter(new LessonTypesAdapter(getContext(), lessonTypes));

                    searchingLessons.setVisibility(View.GONE);
                    lessonsFound.setVisibility(View.VISIBLE);
                }
            });

        }
    }
}
