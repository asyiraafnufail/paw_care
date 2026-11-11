package com.pawcare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawcare.app.data.model.Role
import com.pawcare.app.data.model.User
import com.pawcare.app.data.repository.AuthRepository
import com.pawcare.app.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val authenticatedRole: Role? = null,
    val authenticatedUser: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { current ->
                    current.copy(
                        authenticatedUser = user,
                        authenticatedRole = user?.roleEnum
                    )
                }
            }
        }
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingSession = true) }
            val user = authRepository.getCurrentUser()
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    authenticatedRole = user?.roleEnum,
                    authenticatedUser = user
                )
            }
        }
    }

    fun signIn(email: String, pass: String) {
        val emailCheck = Validators.validateEmail(email)
        val passCheck = Validators.validatePassword(pass)

        if (!emailCheck.isValid || !passCheck.isValid) {
            _uiState.update {
                it.copy(
                    emailError = emailCheck.errorMessage,
                    passwordError = passCheck.errorMessage,
                    generalError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailError = null, passwordError = null, generalError = null) }
            val result = authRepository.signIn(email, pass)
            result.onSuccess { user ->
                val role = user.roleEnum
                if (role == null) {
                    authRepository.signOut()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authenticatedRole = null,
                            authenticatedUser = null,
                            generalError = "Role akun tidak valid. Hubungi admin."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authenticatedRole = role,
                            authenticatedUser = user
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, generalError = error.message ?: "Gagal masuk. Periksa email dan password.")
                }
            }
        }
    }

    fun signUp(email: String, pass: String, confirm: String) {
        val emailCheck = Validators.validateEmail(email)
        val passCheck = Validators.validatePassword(pass)
        val confirmCheck = Validators.validateConfirmPassword(pass, confirm)

        if (!emailCheck.isValid || !passCheck.isValid || !confirmCheck.isValid) {
            _uiState.update {
                it.copy(
                    emailError = emailCheck.errorMessage,
                    passwordError = passCheck.errorMessage,
                    confirmPasswordError = confirmCheck.errorMessage,
                    generalError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    generalError = null
                )
            }
            val result = authRepository.signUp(email, pass)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authenticatedRole = user.roleEnum ?: Role.OWNER,
                        authenticatedUser = user
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, generalError = error.message ?: "Gagal mendaftar.")
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState(isCheckingSession = false) }
            onSignedOut()
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null
            )
        }
    }
}
