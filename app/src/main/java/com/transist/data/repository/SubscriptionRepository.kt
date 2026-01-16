package com.transist.data.repository

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.firestore.FirebaseFirestore
import com.transist.util.sha256
import kotlinx.coroutines.tasks.await
import java.time.Instant


class SubscriptionRepository(private val context: Context) {

    /*private val billingClient: BillingClient

    private var purchaseSuccessListener: (() -> Unit)? = null

    fun setOnPurchaseSuccessListener(listener: () -> Unit) {
        purchaseSuccessListener = listener
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                purchases != null
            ) {
                // Satın alma başarılı
                purchaseSuccessListener?.invoke()
            }

        }

    init {
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .setListener(purchasesUpdatedListener)
            .build()
    }*/

    /*fun connect(onConnected: () -> Unit) {
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
    }*/

    fun checkSubscription(
        email: String,
        onResult: (Boolean, Long) -> Unit
    ) {
        FirebaseFirestore.getInstance().collection("subscriptions")
            .document(email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val expiryTime = snapshot.getString("expiryTime")
                    if (expiryTime == null){
                        onResult(false, 0)
                    } else {
                        val expiryMillis = Instant.parse(expiryTime).toEpochMilli()
                        val currentTime = System.currentTimeMillis()
                        onResult(expiryMillis > currentTime, expiryMillis)
                    }
                } else {
                    Log.d("Subscription debug", "Belge yok")
                    onResult(false, 0) // belge yok → abonelik yok
                }
            }
            .addOnFailureListener {
                Log.d("Subscription debug", "Hata: ${it.message}")
                onResult(false, 0) // hata → abonelik yok gibi davran
            }
    }

}
