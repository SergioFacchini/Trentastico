package com.geridea.trentastico;

import android.app.Application;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.providers.DepartmentsProvider;
import com.geridea.trentastico.utils.AppPreferences;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/*
 * Created with ♥ by Slava on 03/03/2017.
 */

@ReportsCrashes(
    formUri = "http://ideagenesi.com/trentastico/submit.php",
    mode = ReportingInteractionMode.TOAST,
    resToastText = R.string.crash_toast_text
)
public class TrentasticoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
        Cacher.init(this);
        AppPreferences.init(this);
        DepartmentsProvider.loadIfNeeded();

        BugLogger.init();

    }

}
