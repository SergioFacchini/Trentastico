package com.geridea.trentastico.gui.activities;


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.view.MenuItem;

public final class NoMenuSettings implements IMenuSettings {

    public static final NoMenuSettings INSTANCE = new NoMenuSettings();

    private NoMenuSettings() { }

    public static NoMenuSettings getInstance() {
        return INSTANCE;
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[0];
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        //Nothing to bind
    }
}
