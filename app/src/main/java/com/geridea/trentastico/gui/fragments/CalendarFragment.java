
package com.geridea.trentastico.gui.fragments;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.views.CourseTimesCalendar;
import com.geridea.trentastico.network.operations.ILoadingOperation;
import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CalendarFragment extends Fragment {

    @BindView(R.id.calendar)     CourseTimesCalendar calendar;
    @BindView(R.id.loading_bar)  View loader;
    @BindView(R.id.loading_text) TextView loadingText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        ButterKnife.bind(this, view);

        //Binding calendar
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
}
