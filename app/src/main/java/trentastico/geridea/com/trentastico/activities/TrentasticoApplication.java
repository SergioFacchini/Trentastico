package trentastico.geridea.com.trentastico.activities;

import android.app.Application;
import android.view.View;

import trentastico.geridea.com.trentastico.activities.providers.DepartmentsProvider;
import trentastico.geridea.com.trentastico.activities.utils.AppPreferences;

/*
 * Created with ♥ by Slava on 03/03/2017.
 */

public class TrentasticoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AppPreferences.init(this);
        DepartmentsProvider.loadIfNeeded();

    }

}
