package com.geridea.trentastico.gui.activities;


/*
 * Created with ♥ by Slava on 22/03/2017.
 */

import android.support.v4.app.Fragment;

public abstract class FragmentWithMenuItems extends Fragment implements IMenuSettings {

    private HomeActivity activity;

    protected void setActivity(HomeActivity activity) {
        this.activity = activity;
    }

    public void goToCalendarFragment() {
        activity.switchToCalendarFragment();
    }


}
