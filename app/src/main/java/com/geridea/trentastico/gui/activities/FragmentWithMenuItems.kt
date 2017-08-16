package com.geridea.trentastico.gui.activities


/*
 * Created with ♥ by Slava on 22/03/2017.
 */

import android.support.v4.app.Fragment

abstract class FragmentWithMenuItems : Fragment(), IMenuSettings {

    private var activity: HomeActivity? = null

    fun setActivity(activity: HomeActivity) {
        this.activity = activity
    }

    fun goToCalendarFragment() {
        activity!!.switchToCalendarFragment()
    }


}
