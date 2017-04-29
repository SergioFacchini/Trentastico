package com.geridea.trentastico.gui.fragments;


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.model.LibraryOpeningTimes;
import com.geridea.trentastico.network.Networker;
import com.geridea.trentastico.network.request.listener.LibraryOpeningTimesListener;
import com.geridea.trentastico.utils.UIUtils;
import com.geridea.trentastico.utils.time.CalendarUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LibrariesFragment extends FragmentWithMenuItems implements LibraryOpeningTimesListener {

    private static final SimpleDateFormat CURRENT_DAY_FORMAT = new SimpleDateFormat("EEEE dd MMM");
    private static final SimpleDateFormat DAY_NAME = new SimpleDateFormat("EEEE");

    private enum State { LOADING, SHOWING, ERROR }

    @BindView(R.id.loader_spinner) ProgressBar spinner;
    @BindView(R.id.timetables)     TableLayout timetables;
    @BindView(R.id.error_panel)    View errorPanel;

    @BindView(R.id.current_day_name) TextView currentDayName;

    @BindView(R.id.buc_times)        TextView timesBuc;
    @BindView(R.id.cial_times)       TextView timesCial;
    @BindView(R.id.mesiano_times)    TextView timesMesiano;
    @BindView(R.id.povo_times)       TextView timesPovo;
    @BindView(R.id.psicologia_times) TextView timesPsicologia;

    private Calendar currentDay;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_libraries, container, false);
        ButterKnife.bind(this, view);

        currentDay = CalendarUtils.getDebuggableToday();

        showCurrentDay(currentDay);
        loadOpeningTimesForDate(currentDay);

        return view;
    }

    private void showCurrentDay(Calendar currentDay) {
        currentDayName.setText(interpretDay(currentDay));
    }

    private String interpretDay(Calendar currentDay) {
        if (CalendarUtils.isSameDay(currentDay, CalendarUtils.getDebuggableToday())) {
            return String.format("Oggi (%s)", DAY_NAME.format(currentDay.getTime()));
        }

        Calendar tomorrow = CalendarUtils.getDebuggableToday();
        tomorrow.add(Calendar.DAY_OF_MONTH, +1);
        if (CalendarUtils.isSameDay(currentDay, tomorrow)) {
            return String.format("Domani (%s)", DAY_NAME.format(currentDay.getTime()));
        }

        Calendar yesterday = CalendarUtils.getDebuggableToday();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        if(CalendarUtils.isSameDay(currentDay, yesterday)){
            return String.format("Ieri (%s)", DAY_NAME.format(currentDay.getTime()));
        }

        return CURRENT_DAY_FORMAT.format(currentDay.getTime());
    }

    private void loadOpeningTimesForDate(Calendar day) {
        showState(State.LOADING);

        Networker.getLibraryOpeningTimes((Calendar) day.clone(), this);
    }

    private void showState(final State state) {
        UIUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                spinner   .setVisibility(View.GONE);
                timetables.setVisibility(View.GONE);
                errorPanel.setVisibility(View.GONE);

                switch (state){
                    case SHOWING: timetables.setVisibility(View.VISIBLE); break;
                    case ERROR:   errorPanel.setVisibility(View.VISIBLE); break;
                    case LOADING: spinner   .setVisibility(View.VISIBLE); break;
                }
            }
        });
    }

    @OnClick(R.id.previous_day)
    void onPreviousDayButtonClicked() {
        currentDay.add(Calendar.DAY_OF_MONTH, -1);

        showCurrentDay(currentDay);
        loadOpeningTimesForDate(currentDay);
    }

    @OnClick(R.id.next_day)
    void onNextDayButtonClicked() {
        currentDay.add(Calendar.DAY_OF_MONTH, +1);

        showCurrentDay(currentDay);
        loadOpeningTimesForDate(currentDay);
    }

    @OnClick(R.id.retry_fetch_times)
    void onRetryFetchTimesButtonClicked() {
        loadOpeningTimesForDate(currentDay);
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[0];
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        //Nothing to bind
    }

    @Override
    public void onOpeningTimesLoaded(LibraryOpeningTimes times, Calendar date) {
        //We can start loading the current day, then switch to the next day.
        //In this case, we don't want to mess up with dates
        if (currentDay.getTimeInMillis() == date.getTimeInMillis()) {
            showTimes(times);
        }

    }

    private void showTimes(final LibraryOpeningTimes times) {
        UIUtils.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                timesBuc.       setText(times.timesBuc);
                timesCial.      setText(times.timesCial);
                timesMesiano.   setText(times.timesMesiano);
                timesPovo.      setText(times.timesPovo);
                timesPsicologia.setText(times.timesPsicologia);

                showState(State.SHOWING);
            }
        });
    }

    @Override
    public void onOpeningTimesLoadingError() {
        showState(State.ERROR);
    }

    @Override
    public void onErrorParsingResponse(Exception e) {
        BugLogger.logBug("ERROR PARSING LIBRARY OPENING TIMES", e);
        showState(State.ERROR);
    }

}
