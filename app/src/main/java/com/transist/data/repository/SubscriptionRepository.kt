package com.transist.data.repository

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams

class SubscriptionRepository(private val context: Context) {

    private val billingClient: BillingClient
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // Satın alma işlemi tamamlanırsa burada yakalanır
        }

    init {
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .setListener(purchasesUpdatedListener)
            .build()
    }

    fun connect(onConnected: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                }
            }

            override fun onBillingServiceDisconnected() {
                // İstersen yeniden bağlanmayı deneyebilirsin
            }
        })
    }

    fun checkSubscriptionStatus(onResult: (Boolean, Long) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                val purchase = purchases.first()
                val purchaseTime = purchase.purchaseTime
                onResult(true, purchaseTime)
            } else {
                onResult(false, 0)
            }
        }
    }
}
