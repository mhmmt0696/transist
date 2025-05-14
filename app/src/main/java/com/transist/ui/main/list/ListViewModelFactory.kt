package com.transist.ui.main.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.transist.data.repository.StudyRepository

class ListViewModelFactory(
    private val studyRepository: StudyRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListViewModel(studyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}