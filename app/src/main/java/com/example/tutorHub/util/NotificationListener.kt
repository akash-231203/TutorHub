package com.example.tutorHub.util

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.tutorHub.data.classes.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationListener(private val userId: String) {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "NotificationListener"

    private var listenerRegistration: ListenerRegistration? = null

    val notificationsLiveData = MutableLiveData<List<Notification>>(emptyList())
    val unreadCountLiveData = MutableLiveData(0)

    fun startListening() {
        Log.d(TAG, "Starting real-time listener for userId: $userId")

        listenerRegistration = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100) // Limit to recent 100 notifications for performance
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.map { doc ->
                        Notification(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            type = doc.getString("type") ?: "",
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            sessionId = doc.getString("sessionId"),
                            studentId = doc.getString("studentId"),
                            proposedTime = doc.getDate("proposedTime"),
                            createdAt = doc.getDate("createdAt"),
                            read = doc.getBoolean("read") ?: false
                        )
                    }

                    notificationsLiveData.postValue(notifications)

                    val unreadCount = notifications.count { !it.read }
                    unreadCountLiveData.postValue(unreadCount)

                    Log.d(TAG, "Updated notifications: ${notifications.size} total, $unreadCount unread")
                }
            }
    }

    fun stopListening() {
        Log.d(TAG, "Stopping real-time listener for userId: $userId")
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}

