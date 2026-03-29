package com.example.tutorHub.data.classes

import java.util.Date

/**
 * Represents a notification sent to a user.
 * Stored in Firestore collection: notifications
 */
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // session_requested, reschedule_proposed, session_confirmed, session_rejected, question_posted_free, question_posted_paid, solution_posted, session_starting_soon
    val title: String = "",
    val body: String = "",
    val sessionId: String? = null,
    val studentId: String? = null,
    val questionId: String? = null,
    val tutorId: String? = null,
    val proposedTime: Date? = null,
    val createdAt: Date? = null,
    val read: Boolean = false
)
