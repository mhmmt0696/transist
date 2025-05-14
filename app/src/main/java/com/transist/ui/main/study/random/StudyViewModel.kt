package com.transist.ui.main.study.random

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.transist.data.model.EvaluationState
import com.transist.data.remote.response.MisspelledWord
import com.transist.data.repository.ApiRepository
import com.transist.data.repository.StudyRepository
import com.transist.util.hasRealInternetAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(
    private val studyRepo: StudyRepository,
    private val apiRepo: ApiRepository
) : ViewModel() {

    var reverseTranslation: Boolean = false
    lateinit var originalSentenceLanguage : String
    lateinit var originalSentenceLanguageCode : String
    lateinit var translationSentenceLanguage : String
    lateinit var translationSentenceLanguageCode : String
    lateinit var translationSentenceLanguageInNativeLanguage : String

    private val _loadingEvaluation = MutableStateFlow(false)
    val loadingEvaluation: StateFlow<Boolean> = _loadingEvaluation

    private val _blinking = MutableStateFlow(false)
    val blinking: StateFlow<Boolean> = _blinking

    private val _hasInternet = MutableStateFlow(true)
    val hasInternet: StateFlow<Boolean> = _hasInternet

    private var _apiErrorCritical = MutableStateFlow(false)
    val apiErrorCritical: StateFlow<Boolean> = _apiErrorCritical

    private var _apiErrorBasic = MutableStateFlow(false)
    val apiErrorBasic: StateFlow<Boolean> = _apiErrorBasic

    private val _hasEvaluation = MutableStateFlow<Boolean?>(null)
    private val _isValidLanguageCheck = MutableStateFlow<Boolean?>(null)

    val showEvaluationState: StateFlow<EvaluationState> =
        combine(_hasEvaluation, _isValidLanguageCheck) { evaluation, language ->
            EvaluationState(
                hasEvaluation = evaluation,
                isValidLanguage = language,
                allDone = (evaluation != false && language != null)
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, EvaluationState())

    private var _showTarget = MutableStateFlow(false)
    val showTarget: StateFlow<Boolean> = _showTarget

    private var _showHint = MutableStateFlow(false)
    val showHint: StateFlow<Boolean> = _showHint

    private val _explanation = MutableStateFlow<String?>(null)
    val explanation: StateFlow<String?> = _explanation

    private val _wordsAndCorrections = MutableStateFlow<List<MisspelledWord>>(emptyList())
    val wordsAndCorrections: StateFlow<List<MisspelledWord>> = _wordsAndCorrections

    private var _targetTranslation = MutableStateFlow("")
    val targetTranslation: StateFlow<String> = _targetTranslation

    private var _completeSentence = MutableStateFlow("")
    val completeSentence: StateFlow<String> = _completeSentence

    private val _randomSentence = MutableStateFlow<String>("")
    val randomSentence: StateFlow<String> = _randomSentence

    fun setBlinkingSentence(text: String) {
        _randomSentence.value = text
    }

    private val _isValidTranslationCheck = MutableStateFlow<Boolean?>(null)
    val isValidTranslationCheck: StateFlow<Boolean?> = _isValidTranslationCheck

    var wordsOfEvaluation = mutableListOf<String>()

    private var _activeLevel = MutableStateFlow("--")
    val activeLevel: StateFlow<String> = _activeLevel

    fun setActiveLevelState(state: String) {
        _activeLevel.value = state

    }

    fun getRandomSentence(activeLevel: String, languageCode: String, language: String){
        val word = studyRepo.getRandomWord(activeLevel, languageCode)
        viewModelScope.launch {
            apiRepo.getRandomSentence(activeLevel, word, language).collect { result ->
                when (result) {
                    is ApiRepository.GetSentenceResult.Success -> {
                        _randomSentence.value = result.sentence
                        _blinking.value = false
                    }

                    is ApiRepository.GetSentenceResult.Error -> {
                        _apiErrorCritical.value = true
                        _blinking.value = false
                        Log.d ("Debug", result.message)
                        FirebaseCrashlytics.getInstance().log("Error: ${result.message}")
                    }
                }
            }
        }
    }

    fun getEvaluationNoBlank(queryText: String, targetLanguage: String, targetLanguageCode: String){
        _isValidTranslationCheck.value = null
        viewModelScope.launch {
            apiRepo.getEvaluationNoBlank(queryText).collect { result ->
                when (result) {
                    is ApiRepository.GetTranslationResultNoBlank.Success -> {
                        Log.d("Debug", "getEvaluationNoBlank: ${result.isValidTranslationCheck}")
                        _isValidTranslationCheck.value = result.isValidTranslationCheck
                        getWordsInEvaluation(result.feedback, targetLanguage, targetLanguageCode)
                        _explanation.value = result.feedback
                        _showHint.value = false
                        _showTarget.value = false
                        _loadingEvaluation.value = false
                        _hasEvaluation.value = true
                    }

                    is ApiRepository.GetTranslationResultNoBlank.Error -> {
                        _apiErrorCritical.value = true
                        _loadingEvaluation.value = false
                    }
                }
            }
        }
    }

    fun getEvaluationWithBlank(queryText: String, targetLanguage: String, targetLanguageCode: String){
        viewModelScope.launch {
            apiRepo.getEvaluationWithBlank(queryText).collect { result ->
                when (result) {
                    is ApiRepository.GetTranslationResultWithBlank.Success -> {
                        _completeSentence.value = result.completeSentence
                        _explanation.value = result.feedback
                        getWordsInEvaluation(result.feedback, targetLanguage, targetLanguageCode)
                        _showHint.value = true
                        _showTarget.value = false
                        _loadingEvaluation.value = false
                        _hasEvaluation.value = true
                    }

                    is ApiRepository.GetTranslationResultWithBlank.Error -> {
                        _apiErrorCritical.value = true
                        _loadingEvaluation.value = false
                    }
                }
            }
        }
    }

    fun giveTranslation(query: String, targetLanguage: String, targetLanguageCode: String) {
        viewModelScope.launch {
            apiRepo.getTranslation(query).collect { result ->
                when (result) {
                    is ApiRepository.GetTranslationResult.Success -> {
                        _targetTranslation.value = result.translation ?: ""
                        _explanation.value = result.explanation
                        _showHint.value = false
                        _showTarget.value = true
                        getWordsInEvaluation(result.explanation, targetLanguage, targetLanguageCode)
                        _loadingEvaluation.value = false
                        _hasEvaluation.value = true
                        _isValidLanguageCheck.value = true
                    }

                    is ApiRepository.GetTranslationResult.Error -> {
                        _apiErrorCritical.value = true
                        _loadingEvaluation.value = false
                    }
                }
            }
        }
    }

    fun checkLanguage(sentence: String){
        viewModelScope.launch {
            apiRepo.checkLanguage(sentence, translationSentenceLanguage).collect { result ->
                when (result) {
                    is ApiRepository.GetLanguageResult.Success -> {
                        _isValidLanguageCheck.value = result.isValidLanguageCheck
                        if (result.isValidLanguageCheck == true && reverseTranslation == false){
                            getWordsInTranslationSentence(sentence)
                        }

                        if (result.isValidLanguageCheck == false){
                            _showHint.value = false
                            _showTarget.value = false
                        }
                    }

                    is ApiRepository.GetLanguageResult.Error -> {
                        _apiErrorBasic.value = true
                    }
                }
            }
        }
    }

    fun getWordsInEvaluation(explanation: String?, language: String, languageCode: String) {
        wordsOfEvaluation.clear()
        viewModelScope.launch {
            apiRepo.getWordsInEvaluation(explanation, language, languageCode).collect { result ->
                when (result) {
                    is ApiRepository.GetPickedWordsResult.Success -> {
                        wordsOfEvaluation = result.pickedWords
                    }

                    is ApiRepository.GetPickedWordsResult.Error -> {
                        _apiErrorBasic.value = true
                    }
                }
            }
        }


    }

    fun getWordsInTranslationSentence(translationSentence: String) {
        viewModelScope.launch {
            apiRepo.getWordsInTranslationSentence(translationSentence, translationSentenceLanguage).collect { result ->
                when (result) {
                    is ApiRepository.GetMisspelledResult.Success -> {
                        _wordsAndCorrections.value = result.misspelledWords
                    }

                    is ApiRepository.GetMisspelledResult.Error -> {
                        _apiErrorBasic.value = true
                    }
                }
            }
        }

    }

    fun sentenceGetter(){
        _blinking.value = true
        _hasEvaluation.value = false
        _isValidLanguageCheck.value = null
        _hasInternet.value = true
        _apiErrorCritical.value = false
        _apiErrorBasic.value = false
        viewModelScope.launch {
            if (hasRealInternetAccess()) {
                getRandomSentence(activeLevel.value, originalSentenceLanguageCode, originalSentenceLanguage)
            } else {
                _blinking.value = false
                _hasInternet.value = false
            }
        }
    }

    fun checkTranslationSender(i: Int, query: String, translationSentence: String, targetLanguage: String, targetLanguageCode: String){
        _loadingEvaluation.value = true
        _hasEvaluation.value = false
        _isValidLanguageCheck.value = null
        _hasInternet.value = true
        _apiErrorCritical.value = false
        _apiErrorBasic.value = false
        viewModelScope.launch {
            if (hasRealInternetAccess()) {
                if (i==1) {
                    giveTranslation(query, targetLanguage, targetLanguageCode)
                } else if (i==2){
                    checkLanguage(translationSentence)
                    getEvaluationWithBlank(query, targetLanguage, targetLanguageCode)
                } else {
                    checkLanguage(translationSentence)
                    getEvaluationNoBlank(query, targetLanguage, targetLanguageCode)
                }
            } else {
                _hasInternet.value = false
                _loadingEvaluation.value = false
            }
        }
    }

}