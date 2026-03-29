package com.example.tutorHub.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Helper class for testing and debugging notifications.
 * Use this to manually send test notifications and verify the notification system is working.
 */
class NotificationTestHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService()
    private val TAG = "NotificationTestHelper"

    /**
     * Send a test notification to the current user
     */
    suspend fun sendTestNotificationToSelf(): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Log.e(TAG, "No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val notification = mapOf(
                "userId" to currentUserId,
                "type" to "test_notification",
                "title" to "Test Notification",
                "body" to "If you see this, notifications are working! Time: ${Date()}",
                "createdAt" to Date(),
                "read" to false
            )

            val docRef = firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "✓ Test notification sent successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to send test notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if notifications exist in Firestore for current user
     */
    suspend fun checkNotificationsExist(): Result<Int> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                return Result.failure(Exception("No user logged in"))
            }

            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .await()

            val count = snapshot.documents.size
            Log.d(TAG, "Found $count notifications for user: $currentUserId")

            snapshot.documents.forEach { doc ->
                Log.d(TAG, "Notification: ${doc.id}")
                Log.d(TAG, "  Type: ${doc.getString("type")}")
                Log.d(TAG, "  Title: ${doc.getString("title")}")
                Log.d(TAG, "  Body: ${doc.getString("body")}")
                Log.d(TAG, "  Read: ${doc.getBoolean("read")}")
                Log.d(TAG, "  Created: ${doc.getDate("createdAt")}")
            }

            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notifications: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Test sending a question notification to a specific tutor
     */
    suspend fun testQuestionNotification(tutorId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            Log.d(TAG, "Testing question notification to tutor: $tutorId")
            val result = notificationService.notifyTutorOfFreeQuestion(
                tutorId = tutorId,
                questionId = "test_question_${System.currentTimeMillis()}",
                domain = "Test Domain",
                studentId = currentUserId
            )

            if (result.isSuccess) {
                Log.d(TAG, "✓ Test question notification sent successfully")
            } else {
                Log.e(TAG, "✗ Test question notification failed: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception in test: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Test sending a solution notification to a specific student
     */
    suspend fun testSolutionNotification(studentId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            Log.d(TAG, "Testing solution notification to student: $studentId")
            val result = notificationService.notifyStudentOfSolution(
                studentId = studentId,
                questionId = "test_question_${System.currentTimeMillis()}",
                tutorId = currentUserId,
                domain = "Test Domain"
            )

            if (result.isSuccess) {
                Log.d(TAG, "✓ Test solution notification sent successfully")
            } else {
                Log.e(TAG, "✗ Test solution notification failed: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception in test: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all tutors in the system for testing
     */
    suspend fun getAllTutors(): Result<List<Pair<String, String>>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "tutor")
                .get()
                .await()

            val tutors = snapshot.documents.map { doc ->
                val id = doc.id
                val name = doc.getString("name") ?: doc.getString("username") ?: "Unknown"
                id to name
            }

            Log.d(TAG, "Found ${tutors.size} tutors")
            tutors.forEach { (id, name) ->
                Log.d(TAG, "  Tutor: $name ($id)")
            }

            Result.success(tutors)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tutors: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all students in the system for testing
     */
    suspend fun getAllStudents(): Result<List<Pair<String, String>>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .await()

            val students = snapshot.documents.map { doc ->
                val id = doc.id
                val name = doc.getString("name") ?: doc.getString("username") ?: "Unknown"
                id to name
            }

            Log.d(TAG, "Found ${students.size} students")
            students.forEach { (id, name) ->
                Log.d(TAG, "  Student: $name ($id)")
            }

            Result.success(students)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting students: ${e.message}", e)
            Result.failure(e)
        }
    }
}

