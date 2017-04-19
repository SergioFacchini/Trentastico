package com.geridea.trentastico.gui.activities;


/*
 * Created with ♥ by Slava on 22/03/2017.
 */

import android.app.Fragment;
import android.view.MenuItem;

public abstract class IFragmentWithMenuItems extends Fragment {

    private HomeActivity activity;

    public abstract int[] getIdsOfMenuItemsToMakeVisible();
    
    public abstract void bindMenuItem(MenuItem item);

    protected void setActivity(HomeActivity activity) {
        this.activity = activity;
    }

    public void goToCalendarFragment() {
        activity.switchToCalendarFragment();
    }


}
