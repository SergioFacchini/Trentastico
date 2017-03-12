
package trentastico.geridea.com.trentastico.activities.gui.activities;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.alexvasilkov.android.commons.utils.Views;

import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.views.CourseTimesCalendar;


public class CalendarActivity extends AppCompatActivity  {

    private CourseTimesCalendar calendar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        //Calendar
        calendar = Views.find(this, R.id.week_view);

        showCalendar(true);
    }

    /**
     * Shows the calendar and hides the loader
     */
    private void showCalendar(boolean show) {
        Views.find(this, R.id.week_view)   .setVisibility(show ? View.VISIBLE : View.GONE);
        Views.find(this, R.id.progress_bar).setVisibility(show ? View.GONE    : View.VISIBLE);
    }

}
