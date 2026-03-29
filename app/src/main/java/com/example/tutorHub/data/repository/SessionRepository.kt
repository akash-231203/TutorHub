package com.example.tutorHub.data.repository

import android.util.Log
import com.example.tutorHub.data.classes.SessionRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.math.roundToLong

class SessionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    // Explicit region to avoid NOT_FOUND due to wrong region
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService()

    private val TAG = "SessionRepository"

    private fun normalizeToQuarterHour(date: Date): Date {
        val mins = date.time / 60_000
        val rounded = (mins / 15.0).roundToLong() * 15
        return Date(rounded * 60_000)
    }

    /**
     * Ensures the current user's ID token is fresh and valid.
     * This is critical for Cloud Function calls that require authentication.
     */
    private suspend fun ensureAuthTokenIsValid() {
        val currentUser = auth.currentUser
        Log.d(TAG, "ensureAuthTokenIsValid: currentUser = ${currentUser?.uid}")

        if (currentUser != null) {
            try {
                Log.d(TAG, "Refreshing ID token for user: ${currentUser.uid}")
                // Force refresh the ID token to ensure it's valid
                currentUser.getIdToken(true).await()
                Log.d(TAG, "ID token refresh successful")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh ID token: ${e.message}")
                throw Exception("Failed to refresh authentication token: ${e.message}", e)
            }
        } else {
            Log.e(TAG, "User is not authenticated - currentUser is null")
            throw Exception("User is not authenticated. Please log in first.")
        }
    }

    suspend fun createRequest(request: SessionRequest): Result<String> {
        return try {
            Log.d(TAG, "createRequest called for studentId: ${request.studentId}")

            // Ensure authentication token is valid before calling Cloud Function
            ensureAuthTokenIsValid()

            val normalized = request.requestedTime?.let { normalizeToQuarterHour(it) } ?: request.requestedTime
            val currentUser = auth.currentUser

            Log.d(TAG, "Current user UID: ${currentUser?.uid}")
            Log.d(TAG, "Student ID from request: ${request.studentId}")

            // Verify userId is not empty
            val userId = currentUser?.uid ?: ""
            if (userId.isEmpty()) {
                Log.e(TAG, "ERROR: userId is empty! currentUser.uid is null")
                return Result.failure(Exception("User ID is empty. Please log in again."))
            }

            val payload = hashMapOf(
                "studentId" to request.studentId,
                "tutorId" to request.teacherId,
                "domain" to request.domain,
                "requestedTimeMillis" to (normalized?.time ?: throw IllegalArgumentException("requestedTime is required")),
                "durationMinutes" to request.durationMinutes,
                "message" to (request.message ?: ""),
                "userId" to userId  // Ensure this is not empty
            )

            Log.d(TAG, "Calling Cloud Function with payload keys: ${payload.keys}")
            Log.d(TAG, "  studentId: ${payload["studentId"]}")
            Log.d(TAG, "  userId: ${payload["userId"]}")
            Log.d(TAG, "  tutorId: ${payload["tutorId"]}")
            Log.d(TAG, "  domain: ${payload["domain"]}")
            Log.d(TAG, "  durationMinutes: ${payload["durationMinutes"]}")
            Log.d(TAG, "  requestedTimeMillis: ${payload["requestedTimeMillis"]}")

            val result: HttpsCallableResult = functions
                .getHttpsCallable("createSessionRequest")
                .call(payload)
                .await()

            Log.d(TAG, "Cloud Function response received")

            val data = result.data as? Map<*, *>
            val sessionId = data?.get("sessionId") as? String
            if (!sessionId.isNullOrBlank()) {
                Log.d(TAG, "Session created successfully: $sessionId")
                return Result.success(sessionId)
            }
            // If callable returned nothing useful, fallthrough to direct write
            Log.w(TAG, "Empty response from Cloud Function")
            Result.failure(Exception("Empty response from function"))
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            Log.e(TAG, "createRequest error: $msg", e)

            // If function not found or invocation failed due to deployment/region, try direct write fallback
            if (msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("function", ignoreCase = true)) {
                Log.d(TAG, "Cloud Function not found, attempting direct Firestore write")
                try {
                    val current = auth.currentUser
                    if (current == null || current.uid != request.studentId) {
                        Log.e(TAG, "Cannot create session locally: not authenticated as student (current=${current?.uid}, expected=${request.studentId})")
                        return Result.failure(Exception("Cannot create session locally: not authenticated as student"))
                    }
                    // Refresh token for Firestore write
                    Log.d(TAG, "Refreshing token for direct Firestore write")
                    current.getIdToken(true).await()

                    val now = Date()
                    val normalized = request.requestedTime?.let { normalizeToQuarterHour(it) } ?: throw Exception("requestedTime required")
                    val doc = hashMapOf(
                        "studentId" to request.studentId,
                        "teacherId" to request.teacherId,
                        "domain" to request.domain,
                        "requestedTime" to com.google.firebase.Timestamp(normalized),
                        "durationMinutes" to request.durationMinutes,
                        "message" to (request.message ?: ""),
                        "status" to "requested",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val ref = firestore.collection("sessions").add(doc).await()
                    Log.d(TAG, "Session created via direct write: ${ref.id}")

                    // Send notification to tutor about new session request
                    try {
                        val tutorNotif = mapOf(
                            "userId" to request.teacherId,
                            "type" to "session_requested",
                            "title" to "New session request",
                            "body" to "A student requested a session in ${request.domain}",
                            "sessionId" to ref.id,
                            "studentId" to request.studentId,
                            "createdAt" to Date(),
                            "read" to false
                        )
                        firestore.collection("notifications").add(tutorNotif).await()
                        Log.d(TAG, "Tutor notification sent for session request: ${ref.id}")
                    } catch (notifEx: Exception) {
                        Log.w(TAG, "Failed to send tutor notification: ${notifEx.message}")
                        // Don't fail the whole operation if notification fails
                    }

                    return Result.success(ref.id)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to create request via fallback: ${ex.message}", ex)
                    return Result.failure(Exception("Failed to create request via fallback: ${ex.message}", ex))
                }
            }
            // Check if it's an authentication error
            if (msg.contains("unauthenticated", ignoreCase = true) || msg.contains("authentication", ignoreCase = true)) {
                Log.w(TAG, "Authentication error detected in response: $msg")
                return Result.failure(Exception("Authentication failed. Your session may have expired. Please log out and log back in to try again.", e))
            }
            // Otherwise return descriptive error
            val friendly = if (msg.contains("NOT_FOUND", ignoreCase = true)) {
                "Service unavailable: session function not found. Please deploy the Cloud Function 'createSessionRequest' in region us-central1."
            } else msg
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun getRequestsForTeacher(teacherId: String): Result<List<SessionRequest>> {
        return try {
            Log.d(TAG, "getRequestsForTeacher: Fetching requests for teacherId=$teacherId")

            // Read from sessions since server writes there
            val snap = firestore.collection("sessions")
                .whereEqualTo("teacherId", teacherId)
                .get().await()

            Log.d(TAG, "getRequestsForTeacher: Query successful, found ${snap.documents.size} requests")

            val list = snap.documents.map { doc ->
                SessionRequest(
                    id = doc.id,
                    studentId = doc.getString("studentId") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    domain = doc.getString("domain") ?: "",
                    requestedTime = doc.getDate("requestedTime"),
                    durationMinutes = (doc.getLong("durationMinutes") ?: 60L).toInt(),
                    message = doc.getString("message") ?: "",
                    status = doc.getString("status") ?: "requested",
                    rescheduleProposedTime = doc.getDate("rescheduleProposedTime"),
                    rescheduleProposedBy = doc.getString("rescheduleProposedBy"),
                    rejectedBy = doc.getString("rejectedBy"),
                    createdAt = doc.getDate("createdAt"),
                    updatedAt = doc.getDate("updatedAt"),
                    videoRoomId = doc.getString("videoRoomId"),
                    videoSessionStartedAt = doc.getDate("videoSessionStartedAt"),
                    videoSessionEndedAt = doc.getDate("videoSessionEndedAt")
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "getRequestsForTeacher error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRequestsForStudent(studentId: String): Result<List<SessionRequest>> {
        return try {
            val snap = firestore.collection("sessions")
                .whereEqualTo("studentId", studentId)
                .get().await()
            val list = snap.documents.map { doc ->
                SessionRequest(
                    id = doc.id,
                    studentId = doc.getString("studentId") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    domain = doc.getString("domain") ?: "",
                    requestedTime = doc.getDate("requestedTime"),
                    durationMinutes = (doc.getLong("durationMinutes") ?: 60L).toInt(),
                    message = doc.getString("message") ?: "",
                    status = doc.getString("status") ?: "requested",
                    rescheduleProposedTime = doc.getDate("rescheduleProposedTime"),
                    rescheduleProposedBy = doc.getString("rescheduleProposedBy"),
                    rejectedBy = doc.getString("rejectedBy"),
                    createdAt = doc.getDate("createdAt"),
                    updatedAt = doc.getDate("updatedAt"),
                    videoRoomId = doc.getString("videoRoomId"),
                    videoSessionStartedAt = doc.getDate("videoSessionStartedAt"),
                    videoSessionEndedAt = doc.getDate("videoSessionEndedAt")
                )
            }.sortedByDescending { it.createdAt ?: Date(0) } // Sort by newest first
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: String, updatedAt: Date = Date()): Result<Boolean> {
        return try {
            firestore.collection("sessions").document(requestId)
                .update(mapOf("status" to status, "updatedAt" to updatedAt)).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectRequest(requestId: String, rejectedBy: String?): Result<Boolean> {
        return try {
            Log.d(TAG, "rejectRequest called for requestId: $requestId, rejectedBy: $rejectedBy")

            // Ensure authentication token is valid
            ensureAuthTokenIsValid()

            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User is not authenticated"))
            }

            // Fetch the session to verify authorization and get all existing fields
            val sessionDoc = firestore.collection("sessions").document(requestId).get().await()
            if (!sessionDoc.exists()) {
                return Result.failure(Exception("Session not found"))
            }

            val studentId = sessionDoc.getString("studentId") ?: ""
            val teacherId = sessionDoc.getString("teacherId") ?: ""
            val domain = sessionDoc.getString("domain") ?: ""

            // Verify the current user is either the student or teacher
            val isStudent = currentUser.uid == studentId
            val isTeacher = currentUser.uid == teacherId

            if (!isStudent && !isTeacher) {
                return Result.failure(Exception("You are not authorized to reject this session"))
            }

            // Determine rejectedBy based on actual user if not provided
            val actualRejectedBy = rejectedBy ?: if (isStudent) "student" else "teacher"

            val now = Date()

            // Build update with all required fields to satisfy hasOnly constraint
            // We need to include ALL fields that exist in the document
            val updateData = hashMapOf<String, Any>(
                "status" to "rejected",
                "rejectedBy" to actualRejectedBy,
                "updatedAt" to com.google.firebase.Timestamp(now)
            )

            Log.d(TAG, "rejectRequest: Updating session $requestId with rejectedBy=$actualRejectedBy")

            firestore.collection("sessions").document(requestId)
                .update(updateData).await()

            Log.d(TAG, "Session rejected successfully: $requestId")

            // Send notification to the other party
            try {
                val recipientId = if (isStudent) teacherId else studentId
                Log.d(TAG, "Attempting to send rejection notification to recipientId: $recipientId")
                val notifResult = notificationService.notifySessionRejected(
                    recipientId = recipientId,
                    sessionId = requestId,
                    domain = domain,
                    rejectedBy = actualRejectedBy
                )
                if (notifResult.isSuccess) {
                    Log.d(TAG, "✓ Successfully sent rejection notification for session: $requestId")
                } else {
                    Log.e(TAG, "✗ Failed to send rejection notification: ${notifResult.exceptionOrNull()?.message}")
                }
            } catch (notifEx: Exception) {
                Log.e(TAG, "Exception while sending rejection notification: ${notifEx.message}", notifEx)
                // Don't fail the whole operation if notification fails
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "rejectRequest error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun confirmRequest(request: SessionRequest): Result<String> {
        return try {
            Log.d(TAG, "confirmRequest called for requestId: ${request.id}")

            if (request.id.isBlank()) {
                Log.e(TAG, "ERROR: request.id is blank/empty!")
                return Result.failure(Exception("Request ID is empty"))
            }

            // Ensure authentication token is valid
            ensureAuthTokenIsValid()

            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User is not authenticated"))
            }

            // Fetch the session to get all details
            val sessionDoc = firestore.collection("sessions").document(request.id).get().await()
            if (!sessionDoc.exists()) {
                return Result.failure(Exception("Session not found"))
            }

            val studentId = sessionDoc.getString("studentId") ?: ""
            val teacherId = sessionDoc.getString("teacherId") ?: ""

            // Verify the current user is either the student or teacher
            val isStudent = currentUser.uid == studentId
            val isTeacher = currentUser.uid == teacherId

            if (!isStudent && !isTeacher) {
                return Result.failure(Exception("You are not authorized to confirm this session"))
            }

            // Determine the final scheduled time (use reschedule time if available)
            val scheduledTime = request.rescheduleProposedTime ?: request.requestedTime
                ?: return Result.failure(Exception("No scheduled time available"))

            val now = Date()

            // Update the session status to confirmed
            // IMPORTANT: Only use fields allowed by Firestore rules
            // The rules allow: studentId, teacherId, domain, requestedTime, durationMinutes, message, status,
            //                  rescheduleProposedTime, rescheduleProposedBy, rejectedBy, createdAt, updatedAt
            val updateData = hashMapOf<String, Any>(
                "status" to "confirmed",
                "requestedTime" to com.google.firebase.Timestamp(scheduledTime), // Update requestedTime to the final time
                "updatedAt" to com.google.firebase.Timestamp(now)
            )

            firestore.collection("sessions").document(request.id)
                .update(updateData).await()

            Log.d(TAG, "Session confirmed successfully: ${request.id}")

            // Send notification to the other party
            try {
                val recipientId = if (isStudent) teacherId else studentId
                val confirmerRole = if (isStudent) "student" else "tutor"
                val domain = sessionDoc.getString("domain") ?: ""

                val notif = mapOf(
                    "userId" to recipientId,
                    "type" to "session_confirmed",
                    "title" to "Session Confirmed",
                    "body" to "Your $domain session has been confirmed by the $confirmerRole",
                    "sessionId" to request.id,
                    "scheduledTime" to com.google.firebase.Timestamp(scheduledTime),
                    "createdAt" to com.google.firebase.Timestamp(now),
                    "read" to false
                )
                firestore.collection("notifications").add(notif).await()
                Log.d(TAG, "Confirmation notification sent for session: ${request.id}")
            } catch (notifEx: Exception) {
                Log.w(TAG, "Failed to send confirmation notification: ${notifEx.message}")
                // Don't fail the whole operation if notification fails
            }

            Result.success(request.id)
        } catch (e: Exception) {
            Log.e(TAG, "confirmRequest error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun proposeReschedule(requestId: String, proposedTime: Date, proposedBy: String? = null): Result<Boolean> {
        return try {
            val normalized = normalizeToQuarterHour(proposedTime)
            val now = Date()

            // First, fetch the session request to get all required fields
            val sessionDoc = firestore.collection("sessions").document(requestId).get().await()

            if (!sessionDoc.exists()) {
                return Result.failure(Exception("Session not found"))
            }

            val studentId = sessionDoc.getString("studentId") ?: ""
            val teacherId = sessionDoc.getString("teacherId") ?: ""
            val domain = sessionDoc.getString("domain") ?: ""

            // Verify the current user is either the student or teacher
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User is not authenticated"))
            }

            val isStudent = currentUser.uid == studentId
            val isTeacher = currentUser.uid == teacherId

            if (!isStudent && !isTeacher) {
                return Result.failure(Exception("You are not authorized to modify this session"))
            }

            // Determine who is proposing based on actual user if not explicitly provided
            val actualProposedBy = proposedBy ?: if (isStudent) "student" else "teacher"

            // Validate proposedBy matches the actual user
            if ((actualProposedBy == "student" && !isStudent) || (actualProposedBy == "teacher" && !isTeacher)) {
                return Result.failure(Exception("proposedBy does not match authenticated user"))
            }

            // Use Firestore Timestamp for proper type matching with rules
            val updateData = hashMapOf<String, Any>(
                "status" to "reschedule_pending",
                "rescheduleProposedTime" to com.google.firebase.Timestamp(normalized),
                "rescheduleProposedBy" to actualProposedBy,
                "updatedAt" to com.google.firebase.Timestamp(now)
            )

            Log.d(TAG, "proposeReschedule: Updating session $requestId with proposedBy=$actualProposedBy")

            firestore.collection("sessions").document(requestId)
                .update(updateData).await()

            // Send notification to the other party about the reschedule proposal
            try {
                val recipientId = if (isStudent) teacherId else studentId
                val proposerRole = if (isStudent) "student" else "tutor"

                val notif = mapOf(
                    "userId" to recipientId,
                    "type" to "reschedule_proposed",
                    "title" to "Session reschedule proposed",
                    "body" to "Your $proposerRole has proposed a new time for your $domain session",
                    "sessionId" to requestId,
                    "proposedTime" to com.google.firebase.Timestamp(normalized),
                    "createdAt" to com.google.firebase.Timestamp(now),
                    "read" to false
                )
                firestore.collection("notifications").add(notif).await()
                Log.d(TAG, "Notification sent for reschedule proposal: $requestId")
            } catch (notifEx: Exception) {
                Log.w(TAG, "Failed to send reschedule notification: ${notifEx.message}")
                // Don't fail the whole operation if notification fails
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "proposeReschedule error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkConflicts(teacherId: String, start: Date, durationMinutes: Int): Result<Boolean> {
        return try {
            val end = Date(start.time + durationMinutes * 60_000L)
            val snap = firestore.collection("sessions")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("status", "accepted") // Check only accepted sessions
                .whereLessThan("requestedTime", end)
                .whereGreaterThan("requestedTime", start)
                .get().await()

            Log.d(TAG, "checkConflicts: Found ${snap.size()} conflicting sessions")

            Result.success(snap.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
