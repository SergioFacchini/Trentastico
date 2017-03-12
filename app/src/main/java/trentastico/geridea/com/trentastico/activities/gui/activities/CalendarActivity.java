
package trentastico.geridea.com.trentastico.activities.gui.activities;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;

import butterknife.BindView;
import butterknife.ButterKnife;
import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.views.CourseTimesCalendar;


public class CalendarActivity extends AppCompatActivity {

    @BindView(R.id.calendar)    CourseTimesCalendar calendar;
    @BindView(R.id.loading_bar) View loader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        ButterKnife.bind(this);

        //Binding calendar
        calendar.onLoadingOperationStarted.connect(new Listener1<CourseTimesCalendar.CalendarLoadingOperation>() {
            @Override
            public void apply(CourseTimesCalendar.CalendarLoadingOperation operation) {
                loader.setVisibility(View.VISIBLE);
            }
        });

        calendar.onLoadingOperationFinished.connect(new Listener0() {
            @Override
            public void apply() {
                loader.setVisibility(View.GONE);
            }
        });

        calendar.loadNearEvents();
    }

}
