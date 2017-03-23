package com.geridea.trentastico.gui.fragments;


/*
 * Created with ♥ by Slava on 22/03/2017.
 */

import android.app.Fragment;
import android.view.MenuItem;

public abstract class IFragmentWithMenuItems extends Fragment {

    public abstract int[] getIdsOfMenuItemsToMakeVisible();
    
    public abstract void bindMenuItem(MenuItem item);

}
