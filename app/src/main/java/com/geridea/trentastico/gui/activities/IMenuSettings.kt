package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.view.MenuItem

interface IMenuSettings {

    val idsOfMenuItemsToMakeVisible: IntArray

    fun bindMenuItem(item: MenuItem)
}
