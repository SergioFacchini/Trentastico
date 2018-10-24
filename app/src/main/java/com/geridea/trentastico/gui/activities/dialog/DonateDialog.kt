package com.geridea.trentastico.gui.activities.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.geridea.trentastico.Config.BILLING_LICENCE
import com.geridea.trentastico.R
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.DONATION_ITEMS
import com.geridea.trentastico.model.DonationItem
import com.geridea.trentastico.utils.AppPreferences
import kotlinx.android.synthetic.main.dialog_donate.view.*
import kotlinx.android.synthetic.main.itm_donation.view.*


/*
 * Created with ♥ by Slava on 12/10/2018.
 */
class DonateDialog(activity: Activity) : AlertDialog(activity), BillingProcessor.IBillingHandler, DialogInterface.OnDismissListener {

    private var bp: BillingProcessor

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_donate)

        //Setting spinner
        val adapter = createAdapter(context)
        adapter.itemsList = DONATION_ITEMS

        view.offerSpinner.apply {
            this.adapter = adapter

            setSelection(1)

            onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, spinnerView: View?, position: Int, id: Long) {
                    val selectedItem = getItemAtPosition(position) as DonationItem

                    view.confirmButton.text = "OFFRI "+selectedItem.description.toUpperCase()
                }
            }
        }

        bp = BillingProcessor(context, BILLING_LICENCE, this)
        bp.initialize()

        view.confirmButton.setOnClickListener {
            val selectedItem = view.offerSpinner.selectedItem as  DonationItem
            bp.purchase(activity, selectedItem.sku)
        }

        setOnDismissListener(this)
        setView(view)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        bp.release()
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

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        BugLogger.info(message, "BILLING")
    }

    override fun onBillingInitialized() {
        showToast("onBillingInitialized()")
    }

    override fun onPurchaseHistoryRestored() {
        showToast("onPurchaseHistoryRestored()")
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        AppPreferences.donationIconId = getProductInternalId(productId)

        showToast("onProductPurchased($productId, ${details?.toString()})")

    }

    private fun getProductInternalId(productId: String): Int? =
            DONATION_ITEMS.first { it.sku == productId }.internalId


    override fun onBillingError(errorCode: Int, error: Throwable?) {
        showToast("onBillingError($error, ${error?.message})")
    }

}