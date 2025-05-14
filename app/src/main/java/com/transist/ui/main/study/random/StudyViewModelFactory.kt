package com.transist.ui.main.study.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.transist.data.repository.ApiRepository
import com.transist.data.repository.StudyRepository

class StudyViewModelFactory(
    private val studyRepository: StudyRepository,
    private val apiRepository: ApiRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(studyRepository, apiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}