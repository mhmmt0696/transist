package com.transist.data.repository

import android.util.Log
import android.view.View
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

class DailyQuotaRepository (
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    //val hashedEmailWithMethod = getHashedEmailWithMethod()

    fun createInitialDailyQuotaData() {
        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(
            "last_day" to LocalDate.now().toString(),
            "daily_translation_quota" to 10
        )

        firestore.collection("subscriptions")
            .document(uid)
            .set(data, SetOptions.merge())
    }

    fun decreaseDailyTranslationQuota() {
        val uid = auth.currentUser?.uid ?: return
        checkAndResetDailyQuota(){
            val docRef = firestore.collection("subscriptions").document(uid)
            getTranslationCount { count ->
                if (count != 0) {
                    val newCount = mapOf(
                        "daily_translation_quota" to count - 1
                    )
                    docRef.update(newCount)
                }
            }
        }
    }

    fun getTranslationCount(onResult: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(0)
        val providerId = auth.currentUser?.providerData?.lastOrNull()?.providerId
        Log.d ("getTranslationCount 0", "providerId: $providerId")
        if (providerId == EmailAuthProvider.PROVIDER_ID && auth.currentUser?.isEmailVerified == false) {
            Log.d ("getTranslationCount 1", "daily_translation_quota: 0")
            onResult(0)
        } else {
            firestore.collection("subscriptions")
                .document(uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val count = snapshot.getLong("daily_translation_quota")?.toInt() ?: 0
                    val d = snapshot.getLong("daily_translation_quota")?.toInt()
                    Log.d("getTranslationCount 2", "daily_translation_quota: $d")
                    onResult(count)
                }
                .addOnFailureListener {
                    onResult(0)
                }
        }
    }

    fun checkAndResetDailyQuota(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete()
        val today = LocalDate.now()
        val docRef = firestore.collection("subscriptions").document(uid)

        docRef.get().addOnSuccessListener { snapshot ->
            val lastDayString = snapshot.getString("last_day")
            val lastDay = if (lastDayString != null) {
                LocalDate.parse(lastDayString)
            } else {
                LocalDate.of(2000, 1, 1)
            }

            if (lastDay.isBefore(today)) {
                val updates = mapOf(
                    "daily_translation_quota" to 10,
                    "last_day" to today.toString()
                )
                Log.d("Quota debug", "log check 1")
                docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { onComplete() }
            } else {
                onComplete()
            }
        }.addOnFailureListener {
            onComplete()
        }
    }


}