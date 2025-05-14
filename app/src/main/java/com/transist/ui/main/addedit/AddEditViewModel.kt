package com.transist.ui.main.addedit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transist.data.model.DialogState
import com.transist.data.model.ExpressionData
import com.transist.data.model.FolderData
import com.transist.data.remote.response.Sentence
import com.transist.data.repository.ApiRepository
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddEditViewModel(
    private val languageRepo: LanguageRepository,
    private val studyRepo: StudyRepository,
    private val apiRepo: ApiRepository
) : ViewModel() {

    private val _userLanguagesCodes = MutableLiveData<Pair<String, String>>(languageRepo.getUserLanguagesCodes())
    val userLanguagesCodes: LiveData<Pair<String, String>> = _userLanguagesCodes

    private val _userLanguage = MutableLiveData<Pair<String, String>>(languageRepo.getUserLanguages())
    val userLanguages: LiveData<Pair<String, String>> = _userLanguage

    private val _activeFolderName = MutableLiveData<String?>()
    val activeFolderName: LiveData<String?> get() = _activeFolderName

    var activeFolderId = -1

    private val _sentences = MutableStateFlow<MutableList<Sentence>?>(null)
    val sentences: StateFlow<MutableList<Sentence>?> = _sentences

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _isValidLanguage = MutableStateFlow<Boolean?>(null)
    private val _isValidSpelling = MutableStateFlow<Boolean?>(null)
    private val _isValidMeaning = MutableStateFlow<Boolean?>(null)
    private val _isValidCommonness = MutableStateFlow<Boolean?>(null)

    val dialogState: StateFlow<DialogState> =
        combine(_isValidLanguage, _isValidSpelling, _isValidMeaning, _isValidCommonness) { lang, spell, mean, common ->
            DialogState(
                isValidLanguage = lang,
                isValidSpelling = spell,
                isValidMeaning = mean,
                isValidCommonness = common,
                allDone = (lang != null && spell != null && common != null && mean != null)
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DialogState())

    fun initSentences(sentences: MutableList<Sentence>) {
        _sentences.value = sentences
    }

    fun search(expression: String, meaning: String, note: String, targetLanguage: String, nativeLanguage: String) {
        _loading.value = true
        resetChecks()

        viewModelScope.launch {
            // Çoklu API çağrılarını başlatıyoruz
            launch { apiRepo.sendQueryFindSentences(expression, meaning, note, targetLanguage, nativeLanguage)
                .collect { result ->
                    _sentences.value = result
                    _loading.value = false
                }
            }

            launch { apiRepo.sendQueryCheckSpelling(expression, targetLanguage)
                .collect { result ->
                    _isValidSpelling.value = result
                }
            }

            launch { apiRepo.sendQueryCheckCommonness(expression, targetLanguage)
                .collect { result ->
                    _isValidCommonness.value = result
                }
            }

            launch { apiRepo.sendQueryCheckLanguage(expression, targetLanguage)
                .collect { result ->
                    _isValidLanguage.value = result
                }
            }

            // Eğer meaning alanı doluysa ekstra çağrı
            if (meaning.isNotEmpty()) {
                launch { apiRepo.sendQueryCheckMeaning(expression, meaning, targetLanguage, nativeLanguage)
                    .collect { result ->
                        _isValidMeaning.value = result }
                }
            } else {
                _isValidMeaning.value = true
            }
        }
    }

    fun resetChecks() {
        _isValidLanguage.value = null
        _isValidSpelling.value = null
        _isValidMeaning.value = null
        _isValidCommonness.value = null
    }

    fun loadActiveFolder(s: String) {
        val folder = studyRepo.getActiveFolder()
        activeFolderId = folder.second
        if (activeFolderId == -1 ) {
            _activeFolderName.value = s
        } else {
            _activeFolderName.value = folder.first
        }
    }

    fun loadFolders(): List<FolderData> {
        return studyRepo.getAllFoldersByTargetLanguage()
    }

    fun selectFolder(folderId: Int) {
        studyRepo.updateActiveAddFolder(folderId)
        _activeFolderName.value = studyRepo.getFolderNameById(folderId)
        activeFolderId = folderId
    }

    fun createFolder(name: String): Int {
        return studyRepo.createFolder(name)
    }

    fun saveSentences(
        expression: String,
        meaning: String,
        note: String,
        selectedSentences: List<Sentence>,
        folderId: Int
    ) {
        // DB işlemini Repository’ye yönlendir
        studyRepo.saveSelectedSentences(expression, meaning, note, selectedSentences, folderId)
    }

    fun getExpressionByIdNew(id: Int): ExpressionData {
        return studyRepo.getExpressionByIdNew(id)
    }

    fun getFolderNameById(id: Int): String{
        return studyRepo.getFolderNameById(id)
    }

    fun updateSelectedSentences(id: Int, folderId: Int, expression: String, meaning: String, note: String, selectedSentences: MutableList<Sentence>): Int {
        return studyRepo.updateSelectedSentences(id, folderId, expression, meaning, note, selectedSentences)
    }

}