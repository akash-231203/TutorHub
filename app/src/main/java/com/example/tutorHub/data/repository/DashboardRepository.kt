package com.example.tutorHub.data.repository

import android.util.Log
import com.example.tutorHub.data.classes.StudentDashboardData
import com.example.tutorHub.data.classes.TeacherDashboardData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DashboardRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getStudentDashboardData(studentId: String): Result<StudentDashboardData> {
        val tag = "DashboardRepository"

        // Lifetime total posted questions: prefer denormalized field on user doc
        val userDoc = runCatching {
            firestore.collection("users").document(studentId).get().await()
        }.onFailure { Log.w(tag, "fetch user doc failed", it) }.getOrNull()

        val postedQuestionsFromUser = userDoc?.let {
            val asLong = it.getLong("totalQuestionsPosted")
            val asDouble = it.getDouble("totalQuestionsPosted")
            when {
                asLong != null -> asLong.toInt()
                asDouble != null -> asDouble.toInt()
                else -> null
            }
        }

        // Solved questions: lifetime counter on user doc with best-effort fallback
        val solvedFromUser = userDoc?.let {
            val asLong = it.getLong("totalSolvedQuestions")
            val asDouble = it.getDouble("totalSolvedQuestions")
            when {
                asLong != null -> asLong.toInt()
                asDouble != null -> asDouble.toInt()
                else -> null
            }
        }

        val solvedFallbackClosed = runCatching {
            firestore.collection("questions")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "closed")
                .get().await().size()
        }.getOrElse { 0 }
        val solvedFallbackAnswered = runCatching {
            firestore.collection("questions")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "answered")
                .get().await().size()
        }.getOrElse { 0 }
        val solvedQuestions = solvedFromUser ?: (solvedFallbackClosed + solvedFallbackAnswered)

        // Current questions in the collection (not deleted)
        val currentQuestionsCount = runCatching {
            firestore.collection("questions")
                .whereEqualTo("studentId", studentId)
                .get().await().size()
        }.onFailure { Log.w(tag, "currentQuestionsCount query failed", it) }
            .getOrElse { 0 }

        // Total posted = prefer user doc counter, otherwise use current questions + solved (deleted ones)
        val postedQuestions = postedQuestionsFromUser
            ?: (currentQuestionsCount + solvedQuestions)

        val sessionsAttended = runCatching {
            firestore.collection("sessions")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "completed")
                .get().await().size()
        }.onFailure { Log.w(tag, "sessionsAttended query failed", it) }
            .getOrElse { 0 }

        val queriesPending = runCatching {
            firestore.collection("questions")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "open")
                .get().await().size()
        }.onFailure { Log.w(tag, "queriesPending query failed", it) }
            .getOrElse { 0 }

        val reviewsGiven = runCatching {
            firestore.collectionGroup("solutions")
                .whereEqualTo("ratedBy", studentId)
                .get().await().size()
        }.onFailure { Log.w(tag, "reviewsGiven (collectionGroup solutions) query failed", it) }
            .getOrElse { 0 }

        Log.d(tag, "studentId=$studentId posted=$postedQuestions solved=$solvedQuestions attended=$sessionsAttended pending=$queriesPending reviewsGiven=$reviewsGiven")

        return Result.success(
            StudentDashboardData(
                postedQuestions = postedQuestions,
                sessionsAttended = sessionsAttended,
                queriesPending = queriesPending,
                reviewsGiven = reviewsGiven,
                solvedQuestions = solvedQuestions
            )
        )
    }

    suspend fun getTeacherDashboardData(teacherId: String): Result<TeacherDashboardData> {
        val tag = "DashboardRepository"
        val completedSessionsSnapshot = runCatching {
            firestore.collection("sessions")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("status", "completed")
                .get().await()
        }.onFailure { Log.w(tag, "completed sessions query failed", it) }
            .getOrNull()

        val sessionsConducted = completedSessionsSnapshot?.size() ?: 0
        val earnings = completedSessionsSnapshot?.documents?.sumOf { it.getDouble("fee") ?: 0.0 } ?: 0.0

        // First try: read denormalized rating fields from users/{teacherId}
        val userDoc = runCatching {
            firestore.collection("users").document(teacherId).get().await()
        }.onFailure { Log.w(tag, "fetch user doc failed", it) }
            .getOrNull()

        var averageRating = userDoc?.getDouble("ratingAvg") ?: Double.NaN
        // ratingCount might be stored as Long or Double depending on previous writes
        val ratingCountLong = userDoc?.getLong("ratingCount")
        val ratingCountDouble = userDoc?.getDouble("ratingCount")
        var reviewsReceived = when {
            ratingCountLong != null -> ratingCountLong.toInt()
            ratingCountDouble != null -> ratingCountDouble.toInt()
            else -> -1
        }

        // Fallback: aggregate from solutions collectionGroup if not available on user doc
        if (averageRating.isNaN() || reviewsReceived < 0) {
            val solutionsForTutor = runCatching {
                firestore.collectionGroup("solutions")
                    .whereEqualTo("tutorId", teacherId)
                    .get().await().documents
            }.onFailure { Log.w(tag, "solutions collectionGroup for tutor query failed", it) }
                .getOrDefault(emptyList())

            val ratings = solutionsForTutor.mapNotNull { it.getDouble("rating") }
            reviewsReceived = ratings.size
            averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0
        }

        if (averageRating.isNaN()) averageRating = 0.0
        if (reviewsReceived < 0) reviewsReceived = 0

        Log.d(tag, "teacherId=$teacherId sessions=$sessionsConducted earnings=$earnings avgRating=$averageRating reviews=$reviewsReceived")

        return Result.success(
            TeacherDashboardData(
                sessionsConducted = sessionsConducted,
                earnings = earnings,
                averageRating = averageRating,
                reviewsReceived = reviewsReceived
            )
        )
    }
}
