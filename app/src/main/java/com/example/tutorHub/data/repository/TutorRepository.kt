package com.example.tutorHub.data.repository

import com.example.tutorHub.data.Domains
import com.example.tutorHub.data.classes.Tutor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TutorRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getDomains(): Result<List<String>> {
        return try {
            Result.success(Domains.list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTutorDomains(tutorId: String): Result<List<String>> {
        return try {
            val doc = firestore.collection("users").document(tutorId).get().await()
            val domains = (doc.get("domains") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            Result.success(domains)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTutorsByDomain(domain: String): Result<List<Tutor>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "tutor")
                .whereArrayContains("domains", domain)
                .get()
                .await()
            val tutors = snapshot.documents.map { doc ->
                // ratingCount might be stored as Long or Double depending on previous writes
                val ratingCountLong = doc.getLong("ratingCount")
                val ratingCountDouble = doc.getDouble("ratingCount")
                val ratingCount = when {
                    ratingCountLong != null -> ratingCountLong.toInt()
                    ratingCountDouble != null -> ratingCountDouble.toInt()
                    else -> 0
                }

                // ratingAvg might be stored as Double or Long; handle both
                val ratingAvg = doc.getDouble("ratingAvg") ?: doc.getLong("ratingAvg")?.toDouble() ?: 0.0

                Tutor(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    domains = (doc.get("domains") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    ratingAvg = ratingAvg,
                    ratingCount = ratingCount
                )
            }
            Result.success(tutors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRatingToTutor(tutorId: String, rating: Double): Result<Boolean> {
        return try {
            val docRef = firestore.collection("users").document(tutorId)
            firestore.runTransaction { tr ->
                val snap = tr.get(docRef)
                val currentSum = snap.getDouble("ratingSum") ?: 0.0
                val countLong = snap.getLong("ratingCount")
                val countDouble = snap.getDouble("ratingCount")
                val currentCountLong = when {
                    countLong != null -> countLong
                    countDouble != null -> countDouble.toLong()
                    else -> 0L
                }
                val newSum = currentSum + rating
                val newCountLong = currentCountLong + 1L
                val avg = if (newCountLong > 0L) newSum / newCountLong.toDouble() else 0.0
                tr.update(docRef, mapOf(
                    "ratingSum" to newSum,
                    "ratingCount" to newCountLong,
                    "ratingAvg" to avg
                ))
            }.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
