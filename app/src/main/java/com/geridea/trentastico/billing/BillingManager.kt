package com.geridea.trentastico.billing

import android.content.Intent
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.geridea.trentastico.Config
import com.geridea.trentastico.gui.activities.HomeActivity
import com.threerings.signals.Signal2

class BillingManager(private val activity: HomeActivity) : BillingProcessor.IBillingHandler {

    val onBillingError     = Signal2<Int, Throwable?>()
    val onProductPurchased = Signal2<String, TransactionDetails?>()

    private val bp: BillingProcessor = BillingProcessor.newBillingProcessor(
            activity, Config.BILLING_LICENCE, Config.MERCHANT_ID, this
    )

    fun notifyActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean =
            bp.handleActivityResult(requestCode, resultCode, data)


    fun init() {
        bp.initialize()
    }

    override fun onBillingInitialized() {}

    override fun onPurchaseHistoryRestored() {}

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        //Due to the cache of the library of the purchased and consumed products, we have to
        //immediately consume the product to avoid that the users receives an automatic payment
        //confirm when he retries to purchase a product that he/she already purchased in the past.
        if(bp.consumePurchase(productId)) {
            onProductPurchased.dispatch(productId, details)
        }
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        onBillingError.dispatch(errorCode, error)
    }

    fun purchaseItemOneTime(sku: String, payload: String): Boolean =
            bp.purchase(activity, sku, payload)

    fun release() {
        bp.release()
    }

}
