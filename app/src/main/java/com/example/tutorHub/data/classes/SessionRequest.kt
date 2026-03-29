package com.example.tutorHub.data.classes

import java.util.Date

/**
 * Represents a session request from a student to a tutor.
 * Stored in Firestore collection: session_requests
 */
data class SessionRequest(
    val id: String = "",
    val studentId: String = "",
    val teacherId: String = "",
    val domain: String = "",
    val requestedTime: Date? = null,
    val durationMinutes: Int = 60,
    val message: String = "",
    val status: String = "pending", // pending, accepted, rejected, reschedule_pending, confirmed, in_progress, completed
    val rescheduleProposedTime: Date? = null,
    val rescheduleProposedBy: String? = null, // "student" or "teacher" - who proposed the reschedule
    val rejectedBy: String? = null, // "student" or "teacher" - who rejected the session
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    // Video session fields
    val videoRoomId: String? = null, // Unique room ID for video session
    val videoSessionStartedAt: Date? = null, // When the video session actually started
    val videoSessionEndedAt: Date? = null // When the video session ended
)
