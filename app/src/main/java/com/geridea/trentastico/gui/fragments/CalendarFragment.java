
package com.geridea.trentastico.gui.fragments;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.adapters.CourseFilterAdapter;
import com.geridea.trentastico.gui.adapters.PartitioningsAdapter;
import com.geridea.trentastico.gui.views.CourseTimesCalendar;
import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage;
import com.geridea.trentastico.gui.views.requestloader.RequestLoaderView;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.PartitioningCase;
import com.geridea.trentastico.utils.AppPreferences;
import com.geridea.trentastico.utils.time.CalendarUtils;
import com.threerings.signals.Listener1;
import com.threerings.signals.Signal1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;

public class CalendarFragment extends IFragmentWithMenuItems {

    @BindView(R.id.calendar)       CourseTimesCalendar calendar;
    @BindView(R.id.request_loader) RequestLoaderView loaderView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        ButterKnife.bind(this, view);

        //Binding calendar
        calendar.prepareForNumberOfVisibleDays(AppPreferences.getCalendarNumOfDaysToShow());
        calendar.goToDate(CalendarUtils.getDebuggableToday());
        calendar.onLoadingOperationNotify.connect(new Listener1<ILoadingMessage>() {
            @Override
            public void apply(final ILoadingMessage operation) {
                loaderView.processMessage(operation);
            }
        });

        calendar.loadNearEvents();

        return view;
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[]{ R.id.menu_filter, R.id.menu_change_view };
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.menu_filter) {
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    new FilterCoursesDialog(getActivity()).show();
                    return true;
                }
            });
        } else if(item.getItemId() == R.id.menu_change_view){
            //Note we cannot call here calendar.getNumberOfVisibleDays() because this might have
            //been called before onCreate
            item.setIcon(getChangeViewMenuIcon(AppPreferences.getCalendarNumOfDaysToShow()));
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int numOfDays = calendar.rotateNumOfDaysShown();
                    item.setIcon(getChangeViewMenuIcon(numOfDays));
                    AppPreferences.setCalendarNumOfDaysToShow(numOfDays);

                    return true;
                }
            });
        }
    }

    @DrawableRes
    private int getChangeViewMenuIcon(int numOfDays) {
        switch (numOfDays){
            default:
            case 1: return R.drawable.ic_calendar_show_1;
            case 2: return R.drawable.ic_calendar_show_2;
            case 3: return R.drawable.ic_calendar_show_3;
            case 7: return R.drawable.ic_calendar_show_7;
        }
    }

    class FilterCoursesDialog extends AlertDialog {

        @BindView(R.id.courses_list) ListView coursesListView;
        @BindView(R.id.no_courses_text)  TextView noCoursesText;
        @BindView(R.id.yes_courses_text) TextView yesCoursesText;

        private final Collection<LessonType> lessonTypes;
        private final CourseFilterAdapter courseAdapter;

        protected FilterCoursesDialog(@NonNull final Context context) {
            super(context);

            //Inflating the view
            View view = Views.inflate(context, R.layout.dialog_filter_courses);

            ButterKnife.bind(this, view);

            lessonTypes = calendar.getCurrentLessonTypes();

            if (lessonTypes.isEmpty()) {
                yesCoursesText.setVisibility(GONE);
            } else {
                noCoursesText.setVisibility(GONE);
            }


            courseAdapter = new CourseFilterAdapter(context, lessonTypes);
            courseAdapter.onLessonTypeVisibilityChanged.connect(new Listener1<LessonType>() {
                @Override
                public void apply(LessonType lesson) {
                    AppPreferences.setLessonTypesIdsToHide(calculateLessonTypesToHide());
                    if(lesson.applyVisibilityToPartitionings()){
                        AppPreferences.updatePartitioningsToHide(lesson);
                        courseAdapter.notifyDataSetChanged(); //Updating %d of %d shown
                    }

                    calendar.notifyLessonTypeVisibilityChanged();
                }
            });
            courseAdapter.onConfigurePartitioningButtonClicked.connect(new Listener1<LessonType>() {
                @Override
                public void apply(LessonType lessonType) {
                    FilterPartitioningsDialog filterPartitionings = new FilterPartitioningsDialog(context, lessonType);
                    filterPartitionings.onPartitioningVisibilityChanged.connect(new Listener1<LessonType>() {
                        @Override
                        public void apply(LessonType affectedLessonType) {
                            if(affectedLessonType.hasAllPartitioningsInvisible()){
                                affectedLessonType.setVisible(false);
                                AppPreferences.setLessonTypesIdsToHide(calculateLessonTypesToHide());
                            } else if(affectedLessonType.hasAtLeastOnePartitioningVisible()) {
                                affectedLessonType.setVisible(true);
                                AppPreferences.setLessonTypesIdsToHide(calculateLessonTypesToHide());
                            }

                            courseAdapter.notifyDataSetChanged(); //Updating %d of %d shown
                            calendar.notifyLessonTypeVisibilityChanged();
                        }
                    });
                    filterPartitionings.show();
                }
            });
            coursesListView.setAdapter(courseAdapter);

            setView(view);
        }

        private ArrayList<Long> calculateLessonTypesToHide() {
            ArrayList<Long> lessonTypesToHideIds = new ArrayList<>();
            for (LessonType lessonType : lessonTypes) {
                if (!lessonType.isVisible()) {
                    lessonTypesToHideIds.add((long) lessonType.getId());
                }
            }

            return lessonTypesToHideIds;
        }

        @OnClick(R.id.dismiss_button)
        void onDoFilterButtonClicked(){
            dismiss();
        }

    }

    class FilterPartitioningsDialog extends AlertDialog {

        public final Signal1<LessonType> onPartitioningVisibilityChanged = new Signal1<>();

        @BindView(R.id.partitionings_list) ListView partitioningsList;
        @BindView(R.id.introduction_text)  TextView introductionText;

        protected FilterPartitioningsDialog(@NonNull Context context, final LessonType lessonType) {
            super(context);

            View view = Views.inflate(context, R.layout.dialog_filter_partition);
            ButterKnife.bind(this, view);

            calculateIntroductionText(lessonType);
            bindAdapter(context, lessonType);

            setView(view);
        }

        private void bindAdapter(@NonNull Context context, final LessonType lessonType) {
            PartitioningsAdapter adapter = new PartitioningsAdapter(context, lessonType.getPartitioning());
            adapter.onPartitioningVisibilityChanged.connect(new Listener1<PartitioningCase>() {
                @Override
                public void apply(PartitioningCase partitioning) {
                    AppPreferences.updatePartitioningsToHide(lessonType);
                    calendar.notifyLessonTypeVisibilityChanged();

                    onPartitioningVisibilityChanged.dispatch(lessonType);
                }
            });
            partitioningsList.setAdapter(adapter);
        }

        private void calculateIntroductionText(LessonType lessonType) {
            String introductionString = String.format(Locale.CANADA,
                "Gli studenti del corso \"%s\" sono divisi in %d gruppi.\nQui sotto puoi togliere la " +
                "spunta dai gruppi di cui non fai parte per nascondere il relativo orario.",
                    lessonType.getName(), lessonType.getPartitioning().getPartitioningCasesSize()
            );
            introductionText.setText(introductionString);
        }

        @OnClick(R.id.dismiss_button)
        void onDismissButtonPressed(){
            dismiss();
        }

    }


}
