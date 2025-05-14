package com.transist.ui.main.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.transist.data.model.Language
import com.transist.data.repository.AuthRepository
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.PreferencesRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val languageRepo: LanguageRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val _emailVerificationResult = MutableLiveData<Result<Boolean>>()
    val emailVerificationResult: LiveData<Result<Boolean>> = _emailVerificationResult

    private val _signedOut = MutableLiveData<Boolean>()
    val signedOut: LiveData<Boolean> = _signedOut

    private val _signInResult = MutableLiveData<Result<Boolean>>()
    val signInResult: LiveData<Result<Boolean>> = _signInResult

    private val _signUpResult = MutableLiveData<Result<Boolean>>()
    val signUpResult: LiveData<Result<Boolean>> = _signUpResult

    private val _googleSignInResult = MutableLiveData<Result<Boolean>>()
    val googleSignInResult: LiveData<Result<Boolean>> = _googleSignInResult

    private val _userLanguages = MutableLiveData<Pair<String, String>>(languageRepo.getUserLanguagesCodes())
    val userLanguages: LiveData<Pair<String, String>> = _userLanguages

    private val _passwordResetResult = MutableLiveData<Result<Boolean>>()
    val passwordResetResult: LiveData<Result<Boolean>> = _passwordResetResult

    private val _deleteUserState = MutableLiveData<DeleteUserState>()
    val deleteUserState: LiveData<DeleteUserState> = _deleteUserState

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _resetPasswordResult = MutableLiveData<Boolean>()
    val resetPasswordResult: LiveData<Boolean> = _resetPasswordResult

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun getThemeMode(): Int {
        return prefsRepo.getThemeMode()
    }

    fun setThemeMode(mode: Int) {
        prefsRepo.setThemeMode(mode)
    }

    fun sendEmailVerification() {
        authRepo.sendEmailVerification { success, error ->
            if (success){
                _emailVerificationResult.postValue(Result.success(true))
            } else {
                _emailVerificationResult.postValue(Result.failure(Exception(error)))
            }

        }
    }

    fun signOut() {
        authRepo.signOut()
        _signedOut.value = true
    }

    fun signUp(email: String, password: String) {
        authRepo.signUp(email, password) { success, errorCode ->
            if (success) {
                _signUpResult.postValue(Result.success(true))
            } else {
                _signUpResult.postValue(Result.failure(Exception(errorCode ?: "UNKNOWN_ERROR")))
            }
        }
    }

    fun signIn(email: String, password: String) {
        authRepo.signIn(email, password) { success, error ->
            if (success) {
                _signInResult.postValue(Result.success(true))
            } else {
                _signInResult.postValue(Result.failure(Exception(error)))
            }
        }
    }

    fun signInWithGoogleCredential(googleCredential: AuthCredential) {
        authRepo.signInWithGoogleCredential(googleCredential) { success, error ->
            if (success) {
                _googleSignInResult.postValue(Result.success(true))
            } else {
                _googleSignInResult.postValue(Result.failure(Exception(error)))
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        authRepo.sendPasswordResetEmail(email) { success, error ->
            if (success) {
                _passwordResetResult.postValue(Result.success(true))
            } else {
                _passwordResetResult.postValue(Result.failure(Exception(error)))
            }
        }
    }

    fun getListOfLanguages() = languageRepo.getListOfLanguages()

    fun getLanguageListInNative(languageList: List<Language>) = languageRepo.getLanguageListInNative(languageList)

    fun tryDeleteUser() {
        authRepo.deleteUser(
            onSuccess = {
                _deleteUserState.value = DeleteUserState.Success
            },
            onReauthRequired = {
                _deleteUserState.value = DeleteUserState.ReauthRequired
            },
            onError = { msg ->
                _deleteUserState.value = DeleteUserState.Error(msg)
            }
        )
    }

    fun reloadUser() {
        _isLoading.value = true
        authRepo.reloadCurrentUser { user ->
            _currentUser.postValue(user)
            _isLoading.postValue(false)
        }
    }

    fun resetPassword(email: String) {
        authRepo.sendPasswordResetEmail(email) { success ->
            _resetPasswordResult.postValue(success)
        }
    }
}

sealed class DeleteUserState {
    object Success : DeleteUserState()
    object ReauthRequired : DeleteUserState()
    data class Error(val message: String?) : DeleteUserState()
}

