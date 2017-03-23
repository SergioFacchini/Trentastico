
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
import com.geridea.trentastico.gui.views.CourseTimesCalendar;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.network.operations.ILoadingOperation;
import com.geridea.trentastico.utils.AppPreferences;
import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CalendarFragment extends IFragmentWithMenuItems {

    @BindView(R.id.calendar)     CourseTimesCalendar calendar;
    @BindView(R.id.loading_bar)  View loader;
    @BindView(R.id.loading_text) TextView loadingText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        ButterKnife.bind(this, view);

        //Binding calendar
        calendar.prepareForNumberOfVisibleDays(AppPreferences.getCalendarNumOfDaysToShow());
        calendar.goToDate(Calendar.getInstance());
        calendar.onLoadingOperationResult.connect(new Listener1<ILoadingOperation>() {
            @Override
            public void apply(final ILoadingOperation operation) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingText.setText(operation.describe());
                        loader.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        calendar.onLoadingOperationFinished.connect(new Listener0() {
            @Override
            public void apply() {
                loader.setVisibility(View.GONE);
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

        private final Collection<LessonType> lessonTypes;
        @BindView(R.id.courses_list)
        ListView coursesListView;

        protected FilterCoursesDialog(@NonNull Context context) {
            super(context);

            //Inflating the view
            View view = Views.inflate(context, R.layout.dialog_filter_courses);

            ButterKnife.bind(this, view);

            lessonTypes = calendar.getCurrentLessonTypes();

            CourseFilterAdapter courseAdapter = new CourseFilterAdapter(context, lessonTypes);
            courseAdapter.onLessonTypeVisibilityChanged.connect(new Listener1<LessonType>() {
                @Override
                public void apply(LessonType lessonTypeWithChangedVisibility) {
                    AppPreferences.setLessonTypesIdsToHide(calculateLessonTypesToHide());

                    calendar.notifyLessonTypeVisibilityChanged();
                }
            });
            coursesListView.setAdapter(courseAdapter);

            setView(view);
        }

        private ArrayList<Integer> calculateLessonTypesToHide() {
            ArrayList<Integer> activitiesToHideIds = new ArrayList<>();
            for (LessonType lessonType : lessonTypes) {
                if (!lessonType.isVisible()) {
                    activitiesToHideIds.add(lessonType.getId());
                }
            }

            return activitiesToHideIds;
        }

        @OnClick(R.id.dismiss_button)
        void onDoFilterButtonClicked(){
            dismiss();
        }

    }


}
