package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.view.MenuItem

class NoMenuSettings private constructor() : IMenuSettings {

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //Nothing to bind
            Unit

    companion object {

        val instance = NoMenuSettings()
    }
}
