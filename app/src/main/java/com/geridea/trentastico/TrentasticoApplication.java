package com.geridea.trentastico;

import android.app.Application;

import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.providers.DepartmentsProvider;
import com.geridea.trentastico.utils.AppPreferences;

/*
 * Created with ♥ by Slava on 03/03/2017.
 */

public class TrentasticoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Cacher.init(this);
        AppPreferences.init(this);
        DepartmentsProvider.loadIfNeeded();

    }

}
