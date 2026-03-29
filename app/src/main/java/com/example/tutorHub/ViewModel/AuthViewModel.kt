package com.example.tutorHub.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.AuthState
import com.example.tutorHub.data.classes.LoginRequest
import com.example.tutorHub.data.classes.StudentSignupRequest
import com.example.tutorHub.data.classes.TutorSignupRequest
import com.example.tutorHub.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _authState = MutableLiveData(AuthState())
    val authState: LiveData<AuthState> = _authState
    private val _showVerificationDialog = MutableLiveData(false)
    val showVerificationDialog: LiveData<Boolean> = _showVerificationDialog


    fun login(email: String, password: String) {
        _authState.value = _authState.value?.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val result = repository.login(LoginRequest(email, password))
                if (result.isSuccess) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        _authState.value = _authState.value?.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = "User ID not found after login"
                        )
                        return@launch
                    }
                    val roleResult = repository.getUserRole(uid)
                    if (roleResult.isSuccess) {
                        _authState.value = _authState.value?.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            role = roleResult.getOrNull()
                        )
                    } else {
                        _authState.value = _authState.value?.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            role = null,
                            errorMessage = roleResult.exceptionOrNull()?.message
                        )
                    }
                } else {
                    _authState.value = _authState.value?.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = _authState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun signupStudent(name: String, email: String, password: String) {
        _authState.value = _authState.value?.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val result = repository.signupStudent(
                    StudentSignupRequest(name, email, password)
                )
                if (result.isSuccess) {
                    _authState.value = _authState.value?.copy(
                        isLoading = false,
                        isLoggedIn = false
                    )
                    _showVerificationDialog.value = true
                }
                else {
                    _authState.value = _authState.value?.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Signup failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = _authState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun signupTutor(name: String, email: String, password: String, domains: List<String>) {
        _authState.value = _authState.value?.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val result = repository.signupTutor(
                    TutorSignupRequest(name, email, password, domains)
                )
                if (result.isSuccess) {
                    _authState.value = _authState.value?.copy(
                        isLoading = false,
                        isLoggedIn = false
                    )

                    _showVerificationDialog.value = true
                }
                else {
                    _authState.value = _authState.value?.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Signup failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = _authState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun onVerificationDialogDismissed() {
        _showVerificationDialog.value = false
    }

    fun clearError() {
        _authState.value = _authState.value?.copy(errorMessage = null)
    }
}
