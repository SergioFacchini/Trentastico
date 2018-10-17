package com.geridea.trentastico.gui.activities.dialog

import android.app.AlertDialog
import android.content.Context
import android.support.annotation.DrawableRes
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import kotlinx.android.synthetic.main.dialog_donate.view.*
import kotlinx.android.synthetic.main.itm_donation.view.*


/*
 * Created with ♥ by Slava on 12/10/2018.
 */
class DonateDialog(context: Context) : AlertDialog(context) {

    data class DonationItem(val id: String, val description: String, @DrawableRes val resource: Int)

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_donate)

        val adapter = createAdapter(context)
        adapter.itemsList = DONATION_ITEMS
        view.offerSpinner.adapter = adapter
        view.offerSpinner.setSelection(1)

        setView(view)
    }

    private fun createAdapter(context: Context): ItemsAdapter<DonationItem> {
        return object : ItemsAdapter<DonationItem>(context) {

            override fun createView(item: DonationItem, pos: Int, parent: ViewGroup?, inflater: LayoutInflater): View =
                    inflater.inflate(R.layout.itm_donation, parent, false)

            override fun bindView(item: DonationItem, pos: Int, view: View) {
                val icon = ResourcesCompat.getDrawable(context.resources, item.resource, null)
                view.imgDonation.setImageDrawable(icon)
                view.descriptionDonation.text = item.description
            }
        }
    }

    companion object {
        private val DONATION_ITEMS = listOf(
            DonationItem(
                "donation_bad_coffee",
                "Un caffè delle macchinette",
                R.drawable.ic_donation_bad_coffee
            ),
            DonationItem(
                "donation_good_coffee",
                "Un caffè serio",
                R.drawable.ic_donation_good_coffee
            ),
            DonationItem(
                "donation_spritz",
                "Uno Spritz",
                R.drawable.ic_donation_spritz
            ),
            DonationItem(
                "donation_book",
                "Un libro",
                R.drawable.ic_donation_book
            )
        )
    }

}