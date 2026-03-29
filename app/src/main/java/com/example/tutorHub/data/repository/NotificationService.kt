package com.example.tutorHub.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Centralized service for creating and sending notifications to users.
 * This ensures consistent notification handling across the app.
 */
class NotificationService {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "NotificationService"

    /**
     * Send notification when a student posts a free (written) question
     */
    suspend fun notifyTutorOfFreeQuestion(
        tutorId: String,
        questionId: String,
        domain: String,
        studentId: String
    ): Result<Boolean> {
        return try {
            val notification = mapOf(
                "userId" to tutorId,
                "type" to "question_posted_free",
                "title" to "New Question Available",
                "body" to "A new $domain question has been posted",
                "questionId" to questionId,
                "studentId" to studentId,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to tutor $tutorId for free question $questionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send free question notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a student posts a paid (video) question to a specific tutor
     */
    suspend fun notifyTutorOfPaidQuestion(
        tutorId: String,
        questionId: String,
        domain: String,
        studentId: String
    ): Result<Boolean> {
        return try {
            val notification = mapOf(
                "userId" to tutorId,
                "type" to "question_posted_paid",
                "title" to "New Video Request",
                "body" to "You have a new video explanation request for $domain",
                "questionId" to questionId,
                "studentId" to studentId,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to tutor $tutorId for paid question $questionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send paid question notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a tutor posts a solution to a question
     */
    suspend fun notifyStudentOfSolution(
        studentId: String,
        questionId: String,
        tutorId: String,
        domain: String
    ): Result<Boolean> {
        return try {
            val notification = mapOf(
                "userId" to studentId,
                "type" to "solution_posted",
                "title" to "Solution Available",
                "body" to "A tutor has posted a solution to your $domain question",
                "questionId" to questionId,
                "tutorId" to tutorId,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to student $studentId for solution to question $questionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send solution notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a student requests a session
     */
    suspend fun notifyTutorOfSessionRequest(
        tutorId: String,
        sessionId: String,
        domain: String,
        studentId: String,
        requestedTime: Date
    ): Result<Boolean> {
        return try {
            val notification = mapOf(
                "userId" to tutorId,
                "type" to "session_requested",
                "title" to "New Session Request",
                "body" to "A student has requested a $domain session",
                "sessionId" to sessionId,
                "studentId" to studentId,
                "proposedTime" to requestedTime,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to tutor $tutorId for session request $sessionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send session request notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a session is confirmed
     */
    suspend fun notifySessionConfirmed(
        recipientId: String,
        sessionId: String,
        domain: String,
        confirmedBy: String,
        scheduledTime: Date
    ): Result<Boolean> {
        return try {
            val confirmerRole = if (confirmedBy == "tutor") "tutor" else "student"
            val notification = mapOf(
                "userId" to recipientId,
                "type" to "session_confirmed",
                "title" to "Session Confirmed",
                "body" to "Your $domain session has been confirmed by the $confirmerRole",
                "sessionId" to sessionId,
                "proposedTime" to scheduledTime,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to user $recipientId for session confirmation $sessionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send session confirmation notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a session is rejected
     */
    suspend fun notifySessionRejected(
        recipientId: String,
        sessionId: String,
        domain: String,
        rejectedBy: String
    ): Result<Boolean> {
        return try {
            val rejecterRole = if (rejectedBy == "teacher") "tutor" else "student"
            val notification = mapOf(
                "userId" to recipientId,
                "type" to "session_rejected",
                "title" to "Session Declined",
                "body" to "Your $domain session request was declined by the $rejecterRole",
                "sessionId" to sessionId,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to user $recipientId for session rejection $sessionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send session rejection notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a reschedule is proposed
     */
    suspend fun notifyRescheduleProposed(
        recipientId: String,
        sessionId: String,
        domain: String,
        proposedBy: String,
        proposedTime: Date
    ): Result<Boolean> {
        return try {
            val proposerRole = if (proposedBy == "teacher") "tutor" else "student"
            val notification = mapOf(
                "userId" to recipientId,
                "type" to "reschedule_proposed",
                "title" to "Reschedule Proposed",
                "body" to "Your $proposerRole has proposed a new time for your $domain session",
                "sessionId" to sessionId,
                "proposedTime" to proposedTime,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to user $recipientId for reschedule proposal $sessionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reschedule proposal notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification when a session is about to start (e.g., 15 minutes before)
     */
    suspend fun notifySessionStarting(
        userId: String,
        sessionId: String,
        domain: String,
        scheduledTime: Date,
        minutesUntilStart: Int = 15
    ): Result<Boolean> {
        return try {
            val notification = mapOf(
                "userId" to userId,
                "type" to "session_starting_soon",
                "title" to "Session Starting Soon",
                "body" to "Your $domain session starts in $minutesUntilStart minutes",
                "sessionId" to sessionId,
                "proposedTime" to scheduledTime,
                "createdAt" to Date(),
                "read" to false
            )
            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Notification sent to user $userId for upcoming session $sessionId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send session starting notification: ${e.message}", e)
            Result.failure(e)
        }
    }
}

