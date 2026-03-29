package com.example.tutorHub.data.classes

data class LoginRequest(
    val email: String,
    val password: String
)

data class StudentSignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "student"
)

data class TutorSignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val domains: List<String>,
    val role: String = "tutor"
)

enum class UserRole {
    STUDENT, TUTOR
}

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val role: String? = null
)