package com.transist.ui.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.transist.data.repository.AuthRepository
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.PreferencesRepository

class ProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val languageRepository: LanguageRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, languageRepository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
