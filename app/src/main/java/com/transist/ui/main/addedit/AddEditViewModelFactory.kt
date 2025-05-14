package com.transist.ui.main.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.transist.data.repository.ApiRepository
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.StudyRepository

class AddEditViewModelFactory(
    private val languageRepository: LanguageRepository,
    private val studyRepository: StudyRepository,
    private val apiRepository: ApiRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditViewModel(languageRepository, studyRepository, apiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}