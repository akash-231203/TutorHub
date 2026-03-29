package com.example.tutorHub.data.repository

import android.util.Log
import com.example.tutorHub.data.classes.Notification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "NotificationRepository"

    suspend fun getNotificationsForUser(userId: String): Result<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching notifications for userId: $userId")
            val snap = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d(TAG, "Found ${snap.documents.size} notifications")

            val notifications = snap.documents.map { doc ->
                Notification(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    type = doc.getString("type") ?: "",
                    title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "",
                    sessionId = doc.getString("sessionId"),
                    studentId = doc.getString("studentId"),
                    questionId = doc.getString("questionId"),
                    tutorId = doc.getString("tutorId"),
                    proposedTime = doc.getDate("proposedTime"),
                    createdAt = doc.getDate("createdAt"),
                    read = doc.getBoolean("read") ?: false
                )
            }
            Result.success(notifications)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationCount(userId: String): Result<Int> {
        return try {
            val snap = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()
            Result.success(snap.documents.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching unread count: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
            Log.d(TAG, "Marked notification as read: $notificationId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Boolean> {
        return try {
            val snap = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            for (doc in snap.documents) {
                doc.reference.update("read", true).await()
            }
            Log.d(TAG, "Marked ${snap.documents.size} notifications as read")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Boolean> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            Log.d(TAG, "Deleted notification: $notificationId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteOldNotifications(userId: String, daysOld: Int = 30): Result<Boolean> {
        return try {
            val cutoffDate = Date(System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L))
            val snap = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereLessThan("createdAt", cutoffDate)
                .get()
                .await()

            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
            Log.d(TAG, "Deleted ${snap.documents.size} old notifications")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting old notifications: ${e.message}", e)
            Result.failure(e)
        }
    }
}
