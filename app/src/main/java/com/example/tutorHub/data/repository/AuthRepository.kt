package com.example.tutorHub.data.repository

import com.example.tutorHub.data.classes.LoginRequest
import com.example.tutorHub.data.classes.StudentSignupRequest
import com.example.tutorHub.data.classes.TutorSignupRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun login(loginRequest: LoginRequest): Result<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(
                loginRequest.email,
                loginRequest.password
            ).await()

            val user = result.user ?: throw Exception("User not found!")

            if (!user.isEmailVerified) {
                return Result.failure(Exception("Email not verified!"))
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRole(uid: String): Result<String> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val role = doc.getString("role") ?: return Result.failure(Exception("Role not found for user"))
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun signupStudent(request: StudentSignupRequest): Result<Boolean> {
        return try {
            val result = auth.createUserWithEmailAndPassword(request.email, request.password).await()
            val user = result.user ?: throw Exception("User not found")

            val studentData = mapOf(
                "name" to request.name,
                "email" to request.email,
                "role" to "student"
            )
            firestore.collection("users").document(user.uid).set(studentData).await()

            user.sendEmailVerification().await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun signupTutor(request: TutorSignupRequest): Result<Boolean> {
        return try {
            val result = auth.createUserWithEmailAndPassword(request.email, request.password).await()
            val user = result.user ?: throw Exception("User not found")

            val studentData = mapOf(
                "name" to request.name,
                "email" to request.email,
                "role" to "tutor",
                "domains" to request.domains
            )
            firestore.collection("users").document(user.uid).set(studentData).await()

            user.sendEmailVerification().await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
