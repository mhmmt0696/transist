package com.transist.ui.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.transist.data.repository.AuthRepository
import com.transist.data.repository.DailyQuotaRepository
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.PreferencesRepository
import com.transist.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val languageRepo = LanguageRepository(application)
    private val prefsRepo = PreferencesRepository(application)
    private val subsRepo = SubscriptionRepository(application)
    private val quotaRepo = DailyQuotaRepository()
    private val authRepo = AuthRepository()

    private var activeDeviceListener: ValueEventListener? = null

    var appSetId: String = ""
    lateinit var fireBaseUID: String

    private val _signedOut = MutableLiveData<Boolean>()
    val signedOut: LiveData<Boolean> = _signedOut

    private val _userLanguagesCodes = MutableStateFlow(languageRepo.getUserLanguagesCodes())
    val userLanguagesCodes: StateFlow<Pair<String, String>> = _userLanguagesCodes

    val _userLanguages = MutableStateFlow(languageRepo.getUserLanguages())
    val userLanguages: StateFlow<Pair<String, String>> = _userLanguages

    private val _isSubscribed = MutableStateFlow(Pair(false, LocalDateTime.now()))
    val isSubscribed: StateFlow<Pair<Boolean, LocalDateTime>> = _isSubscribed

    private val _daily_translation_quota = MutableLiveData<Int>(0)
    val daily_translation_quota: LiveData<Int?> = _daily_translation_quota

    private val _initial_translation_quota = MutableLiveData<Int>(prefsRepo.getInitalTranslationQuota())
    val initial_translation_quota: LiveData<Int?> = _initial_translation_quota

    private val _activeDeviceId = MutableStateFlow<String?>("null")
    val activeDeviceId: StateFlow<String?> = _activeDeviceId

    private val db = FirebaseDatabase.getInstance().reference
    fun listenActiveDeviceChanges() {
        val ref = db.child("users").child(fireBaseUID).child("activeDeviceId")

        // Daha önce bir listener varsa, önce kaldır
        activeDeviceListener?.let { ref.removeEventListener(it) }

        activeDeviceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newValue = snapshot.getValue(String::class.java)
                Log.e("RTDB", "Dinleme başlatıldı: $newValue")
                _activeDeviceId.value = newValue
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RTDB", "Dinleme iptal edildi: ${error.message}")
            }
        }

        ref.addValueEventListener(activeDeviceListener!!)

    }

    fun stopActiveDeviceListening() {
        val ref = db.child("users").child(fireBaseUID).child("activeDeviceId")
        activeDeviceListener?.let {
            ref.removeEventListener(it)
            Log.e("RTDB", "Dinleme durduruldu")
        }
        activeDeviceListener = null
    }

    fun setAppSetId(context: Context, callback: () -> Unit) {
        val client = AppSet.getClient(context)

        client.appSetIdInfo
            .addOnSuccessListener { info: AppSetIdInfo ->
                appSetId = info.id            // Benzersiz ID
                val scope = info.scope        // AppSetIdInfo.SCOPE_APP veya SCOPE_DEVELOPER
                Log.d("AppSetId", "AppSetId: $appSetId, Scope: $scope")
                callback()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback()
            }
    }

    fun signOut() {
        authRepo.signOut()
        _signedOut.value = true
    }

    fun setSubscriptionUnknown(){
        _isSubscribed.value = Pair(false, LocalDateTime.now())
    }

    fun checkSubscription() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.d("Subscription debug", "User ID is null")
            _isSubscribed.value = Pair(false, LocalDateTime.now())
            return
        }

        subsRepo.checkSubscription(uid) { isActive, expiryTime ->
            val instant = Instant.ofEpochMilli(expiryTime)
            val expiryDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            Log.d("Subscription debug", "isActive: $isActive, expiryDate: $expiryDate")
            _isSubscribed.value = Pair(isActive, expiryDate)
        }

    }

    fun setDailyTranslationQuotaSilently(count: Int){
        _daily_translation_quota.postValue(count)
    }

    fun decreaseTranslationQuota() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            val decreasedQuota = prefsRepo.decreaseInitalTranslationQuota()
            _initial_translation_quota.postValue(decreasedQuota)
        } else {
            quotaRepo.decreaseDailyTranslationQuota()
            _daily_translation_quota.postValue(_daily_translation_quota.value?.minus(1) ?: 0)
            Log.d("dodecrease", "daily decrease quota")
        }
    }

    fun checkAndResetDailyQuota() {
        quotaRepo.checkAndResetDailyQuota {
            quotaRepo.getTranslationCount { count ->
                _daily_translation_quota.postValue(count)
            }
        }
    }

    fun updateUserLanguageCodes(native: String, target: String) {
        languageRepo.updateLanguages(native, target)
        _userLanguages.value = languageRepo.getUserLanguages()
        _userLanguagesCodes.value = Pair(native, target)
    }

    // Hangi fragment gösterilecek
    val selectedFragment = MutableLiveData<String>() // "studyFolder", "studyRandom", "list", "profile"

    fun loadInitialFragment() {
        selectedFragment.postValue("studyRandom")
    }

    fun onBottomNavClicked(tag: String) {
        selectedFragment.postValue(tag)
    }
}
