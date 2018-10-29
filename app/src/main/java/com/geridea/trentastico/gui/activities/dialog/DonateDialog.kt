package com.geridea.trentastico.gui.activities.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.anjlab.android.iab.v3.Constants
import com.geridea.trentastico.R
import com.geridea.trentastico.billing.BillingManager
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.DONATION_ITEMS
import com.geridea.trentastico.model.DonationItem
import com.geridea.trentastico.model.findDonationItemByProductId
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.UIUtils
import com.threerings.signals.Signal1
import kotlinx.android.synthetic.main.dialog_donate.view.*
import kotlinx.android.synthetic.main.itm_donation.view.*


/*
 * Created with ♥ by Slava on 12/10/2018.
 */
class DonateDialog(activity: Activity, private val billingManager: BillingManager)
    :AlertDialog(activity) {

    /**
     * Dispatched when the user has donated and closed the following thank you page. Dispatches
     * the donated product id.
     */
    val onCloseAfterDonation = Signal1<Int>()

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_donate)

        //Prevents the user from unexpectedly close the window when using the app
        setCanceledOnTouchOutside(false)

        setupDonationPart(view)
        setupThankYouPart(view)

        setView(view)
    }

    private fun setupDonationPart(view: View) {
        val adapter = createAdapter(context)
        adapter.itemsList = DONATION_ITEMS
        view.offerSpinner.apply {
            this.adapter = adapter

            setSelection(1)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, spinnerView: View?, position: Int, id: Long) {
                    val selectedItem = getItemAtPosition(position) as DonationItem

                    view.confirmButton.text = "OFFRI " + selectedItem.description.toUpperCase()
                }
            }
        }

        //Setting billing manager
        billingManager.onProductPurchased.connect { productId, _ ->
            val donationProduct = findDonationItemByProductId(productId)
            AppPreferences.donationIconId = donationProduct.internalId

            Networker.sendDonationNotify(donationProduct.description, AppPreferences.studyCourse.toString())

            UIUtils.runOnMainThread {
                setCancelable(false)

                view.donationPart.visibility = View.GONE
                view.thankYouPart.visibility = View.VISIBLE
            }
        }

        billingManager.onBillingError.connect { errorCode, throwable ->
            val message = when (errorCode) {
                Constants.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                Constants.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE ->
                    "Il servizio di pagamento di Google è termporaneamente non disponibile"

                Constants.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE ->
                    "L'oggetto non è più disponibile. Si prega di aggiornare Trentastico ad una versione più recente."

                Constants.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> {
                    BugLogger.logBug("BILLING_RESPONSE_RESULT_DEVELOPER_ERROR", Exception(throwable))
                    "Si è verificato un errore. Si prega di aggiornare Trentastico ad una versione più recente."
                }
                else -> null
            }

            if(message != null){
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        view.confirmButton.setOnClickListener {
            val selectedItem = view.offerSpinner.selectedItem as DonationItem
            val payload = AppPreferences.studyCourse.toString()
            billingManager.purchaseItemOneTime(selectedItem.sku, payload)
        }
    }

    private fun setupThankYouPart(view: View) {
        view.disableDonationDialogs.setOnCheckedChangeListener { _, checked ->
            AppPreferences.showDonationPopups = checked
        }

        view.dismissButton.setOnClickListener {
            dismiss()
            onCloseAfterDonation.dispatch(AppPreferences.donationIconId)
        }
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

}