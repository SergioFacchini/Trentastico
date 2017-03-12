
package trentastico.geridea.com.trentastico.activities.gui.activities;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.views.CourseTimesCalendar;


public class CalendarActivity extends AppCompatActivity  {

    @BindView(R.id.calendar)     CourseTimesCalendar calendar;
    @BindView(R.id.progress_bar) ProgressBar loader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ButterKnife.bind(this);

        showCalendar(true);
    }

    /**
     * Shows the calendar and hides the loader. Note that making the calendar visible makes it
     */
    private void showCalendar(boolean show) {
        calendar.setVisibility(show ? View.VISIBLE : View.GONE);
        loader  .setVisibility(show ? View.GONE    : View.VISIBLE);
    }



}
