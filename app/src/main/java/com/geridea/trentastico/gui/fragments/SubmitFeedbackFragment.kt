package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 18/04/2017.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems

class SubmitFeedbackFragment : FragmentWithMenuItems() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_submit_bug, container, false)

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //We're not using any menu item
            Unit

}
