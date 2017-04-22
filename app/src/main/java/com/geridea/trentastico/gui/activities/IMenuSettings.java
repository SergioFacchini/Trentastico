package com.geridea.trentastico.gui.activities;


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.view.MenuItem;

public interface IMenuSettings {

    int[] getIdsOfMenuItemsToMakeVisible();

    void bindMenuItem(MenuItem item);
}
