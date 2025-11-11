package br.edu.utfpr.cc.setac.wotsiapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    val isUserLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.signUp(email, password, displayName)
            _loading.value = false

            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.SignUpSuccess(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                }
            )
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.signIn(email, password)
            _loading.value = false

            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.SignInSuccess(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.SignedOut
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.sendPasswordResetEmail(email)
            _loading.value = false

            result.fold(
                onSuccess = {
                    _authState.value = AuthState.PasswordResetEmailSent
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Failed to send reset email")
                }
            )
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _loading.value = true
            val result = authRepository.resendVerificationEmail()
            _loading.value = false

            result.fold(
                onSuccess = {
                    _authState.value = AuthState.VerificationEmailSent
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Failed to send verification email")
                }
            )
        }
    }

    fun reloadUser() {
        viewModelScope.launch {
            authRepository.reloadUser()
            currentUser?.let { user ->
                _authState.value = if (user.isEmailVerified) {
                    AuthState.EmailVerified
                } else {
                    AuthState.EmailNotVerified
                }
            }
        }
    }

    sealed class AuthState {
        data class SignUpSuccess(val user: FirebaseUser) : AuthState()
        data class SignInSuccess(val user: FirebaseUser) : AuthState()
        object SignedOut : AuthState()
        object PasswordResetEmailSent : AuthState()
        object VerificationEmailSent : AuthState()
        object EmailVerified : AuthState()
        object EmailNotVerified : AuthState()
        data class Error(val message: String) : AuthState()
    }
}

