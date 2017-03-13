
package trentastico.geridea.com.trentastico.activities.gui.activities;

/*
 * Created with ♥ by Slava on 11/03/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.threerings.signals.Listener0;
import com.threerings.signals.Listener1;

import butterknife.BindView;
import butterknife.ButterKnife;
import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.gui.views.CourseTimesCalendar;
import trentastico.geridea.com.trentastico.activities.network.operations.ILoadingOperation;

public class CalendarActivity extends AppCompatActivity {

    @BindView(R.id.calendar)     CourseTimesCalendar calendar;
    @BindView(R.id.loading_bar)  View loader;
    @BindView(R.id.loading_text) TextView loadingText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        ButterKnife.bind(this);

        //Binding calendar
        calendar.onLoadingOperationResult.connect(new Listener1<ILoadingOperation>() {
            @Override
            public void apply(final ILoadingOperation operation) {
                runOnUiThread(new Runnable() {
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
    }

}
