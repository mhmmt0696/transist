package com.transist.ui.welcome

import androidx.lifecycle.ViewModel
import com.transist.data.model.Language
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.PreferencesRepository
import java.util.Locale

class WelcomeViewModel(
    private val languageRepo: LanguageRepository,
    private val prefRepo: PreferencesRepository
) : ViewModel() {

    // At the first startup, we assume the device language is the user's native language.
    val deviceLanguageCode = Locale.getDefault().language
    var nativeLanguageCode = deviceLanguageCode

    // At the first start, we assume the target language is "en", if "en" is the native language then we assume it is "un".
    var targetLanguageCode: String = if (nativeLanguageCode != "en") "en" else "un"

    fun getListOfLanguages(): List<Language> {
        return languageRepo.getListOfLanguages()
    }

    fun getLanguageListInNative(languageList: List<Language>) = languageRepo.getLanguageListInNative(languageList)

    fun saveLanguages() {
        languageRepo.updateLanguages(nativeLanguageCode, targetLanguageCode)
    }

    fun setFirstLaunchDone() {
        prefRepo.setFirstLaunchDone()
    }
}
