package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.utils.DebugUtils

class AboutFragment : FragmentWithMenuItems() {

    @BindView(R.id.version_text)  lateinit var versionText: TextView

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_about, container, false)
        ButterKnife.bind(this, view)

        versionText.text = DebugUtils.computeVersionName()

        val paperboyFragment = PaperboyFragmentMaker.buildPaperboyFragment(context)
        childFragmentManager
                .beginTransaction()
                .replace(R.id.paperboy_fragment_frame, paperboyFragment)
                .commit()

        return view
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //No menus used
            Unit

}
