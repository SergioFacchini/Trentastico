package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.alexvasilkov.android.commons.utils.Views
import com.amitshekhar.DebugDB
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.DebugUtils
import kotlinx.android.synthetic.main.dialog_licenes.view.*
import kotlinx.android.synthetic.main.fragment_about.view.*

class AboutFragment : FragmentWithMenuItems() {

    var debugClickCounter = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        view.versionText.text = DebugUtils.computeVersionName()

        //DEBUG MODE
        view.slava.setOnClickListener {
            countDebugClick(view)
        }

        view.licencesBtn.setOnClickListener {
            LicencesDialog(requireContext()).show()
        }

        val paperboyFragment = PaperboyFragmentMaker.buildPaperboyFragment(requireContext())
        childFragmentManager
                .beginTransaction()
                .replace(R.id.paperboy_fragment_frame, paperboyFragment)
                .commit()

        return view
    }

    private fun countDebugClick(view: View) {
        debugClickCounter++

        if (AppPreferences.debugIsInDebugMode) {
            DebugDB.initialize(context)
            view.slava.text = DebugDB.getAddressLog()
        }

        if (debugClickCounter % 12 == 0) {
            AppPreferences.debugIsInDebugMode = !AppPreferences.debugIsInDebugMode

            if (AppPreferences.debugIsInDebugMode) {
                Toast.makeText(context, "Ora sei uno sviluppatore! :o", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Ora sei un essere umano qualunque! :o", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = /*No menus used*/ Unit

}


class LicencesDialog(context: Context) : AlertDialog(context) {

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_licenes)
        view.licencesTV.text = context.getText(R.string.licence)

        setView(view)
    }

}

