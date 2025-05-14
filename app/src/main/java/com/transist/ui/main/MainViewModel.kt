package com.transist.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.PreferencesRepository
import com.transist.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val languageRepo = LanguageRepository(application)
    private val prefsRepo = PreferencesRepository(application)
    private val subsRepo = SubscriptionRepository(application)

    private val _userLanguagesCodes = MutableStateFlow(languageRepo.getUserLanguagesCodes())
    val userLanguagesCodes: StateFlow<Pair<String, String>> = _userLanguagesCodes

    val _userLanguages = MutableStateFlow(languageRepo.getUserLanguages())
    val userLanguages: StateFlow<Pair<String, String>> = _userLanguages

    private val _isSubscribed = MutableStateFlow(Pair(false, LocalDateTime.now()))
    val isSubscribed: StateFlow<Pair<Boolean, LocalDateTime>> = _isSubscribed

    private val _translation_count = MutableLiveData<Int>(prefsRepo.getTranslationQuota())
    val translation_count: LiveData<Int?> = _translation_count

    fun checkSubscription() {
        subsRepo.connect {
            subsRepo.checkSubscriptionStatus { isSubscribed, purchaseTime ->
                if (isSubscribed) {
                    // Abonelik aktif
                    val startInstant = Instant.ofEpochMilli(purchaseTime)
                    val startDate = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault())
                    val expiryDate = startDate.plusMonths(1)
                    _isSubscribed.value = Pair(true, expiryDate)
                    Log.d ("Subscription", "Abonelik başlangıç tarihi: $startDate")
                    Log.d ("Subscription", "Abonelik bitiş tarihi: $expiryDate")
                } else {
                    // Abonelik yok
                    _isSubscribed.value = Pair(false, LocalDateTime.now())
                    Log.d ("Subscription", "Abonelik yok")
                }
            }
        }
    }

    fun decreaseTranslationCount() {
        prefsRepo.decreaseTranslationCount()
        _translation_count.postValue(prefsRepo.getTranslationQuota())
    }

    fun checkAndResetDailyQuota() {
        prefsRepo.checkAndResetDailyQuota()
        _translation_count.postValue(prefsRepo.getTranslationQuota())
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
