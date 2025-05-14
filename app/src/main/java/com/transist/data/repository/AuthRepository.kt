package com.transist.data.repository

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser

class AuthRepository (private val auth: FirebaseAuth)  {

    fun reloadCurrentUser(onComplete: (FirebaseUser?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(null)
            return
        }
        user.reload().addOnCompleteListener {
            onComplete(auth.currentUser)
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.e("Firebase", "Kayıt hatası: ${task.exception}")
                    val errorCode = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> (task.exception as? FirebaseAuthWeakPasswordException)?.errorCode
                        is FirebaseAuthInvalidCredentialsException -> (task.exception as? FirebaseAuthInvalidCredentialsException)?.errorCode
                        is FirebaseAuthUserCollisionException -> (task.exception as? FirebaseAuthUserCollisionException)?.errorCode
                        is FirebaseException -> {
                            // Genellikle burada PASSWORD_DOES_NOT_MEET_REQUIREMENTS gibi özel mesaj var
                            if ((task.exception as? FirebaseException)?.message?.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS") == true) {
                                "ERROR_INVALID_PASSWORD"
                            } else {
                                "Bilinmeyen bir hata oluştu."
                            }
                        }
                        else -> "Bilinmeyen bir hata oluştu."
                    }
                    onResult(false, errorCode)
                }
            }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun deleteUser(
        onSuccess: () -> Unit,
        onReauthRequired: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val user = auth.currentUser
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.signOut()
                    onSuccess()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthRecentLoginRequiredException) {
                        onReauthRequired()
                    } else {
                        onError(exception?.message)
                    }
                }
            }
    }

    fun signInWithGoogleCredential(googleCredential: AuthCredential, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithCredential(googleCredential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.e("hata", "Google ile giriş başarısız: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.e("Firebase", "Sıfırlama hatası: ${task.exception}")
                    onResult(false, task.exception?.message)
                }
            }
    }
}
