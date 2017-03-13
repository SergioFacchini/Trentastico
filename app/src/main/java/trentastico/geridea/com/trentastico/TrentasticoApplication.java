package trentastico.geridea.com.trentastico;

import android.app.Application;

import trentastico.geridea.com.trentastico.database.Cacher;
import trentastico.geridea.com.trentastico.network.Networker;
import trentastico.geridea.com.trentastico.providers.DepartmentsProvider;
import trentastico.geridea.com.trentastico.utils.AppPreferences;

/*
 * Created with ♥ by Slava on 03/03/2017.
 */

public class TrentasticoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Cacher.init(this);
        Networker.init(this);
        AppPreferences.init(this);
        DepartmentsProvider.loadIfNeeded();

    }

}
