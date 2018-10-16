package com.geridea.trentastico.gui.activities.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.View
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R


/*
 * Created with ♥ by Slava on 12/10/2018.
 */
class DonateDialog(context: Context) : AlertDialog(context) {

    init {
        Views.inflate<View>(context, R.layout.dialog_donate)

    }

}