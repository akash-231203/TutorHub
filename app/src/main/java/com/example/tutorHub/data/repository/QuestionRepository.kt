package com.example.tutorHub.data.repository

import android.net.Uri
import android.util.Log
import com.example.tutorHub.data.classes.QuestionPost
import com.example.tutorHub.data.classes.Solution
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date

class QuestionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val notificationService = NotificationService()
    private val TAG = "QuestionRepository"

    suspend fun postQuestion(question: QuestionPost): Result<Boolean> {
        return try {
            val docRef = firestore.collection("questions").document()
            val questionWithId = question.copy(id = docRef.id, timestamp = Date())

            val batch = firestore.batch()
            batch.set(docRef, questionWithId)
            // Increment persistent counter of total questions posted by the student, merging if user doc is missing
            val userRef = firestore.collection("users").document(question.studentId)
            batch.set(userRef, mapOf("totalQuestionsPosted" to FieldValue.increment(1)), SetOptions.merge())
            batch.commit().await()

            // Send notification to tutor(s) based on question type
            try {
                if (question.type == "video" && !question.targetTutorId.isNullOrBlank()) {
                    // Paid question - notify specific tutor
                    Log.d(TAG, "Attempting to send paid question notification to tutor: ${question.targetTutorId}")
                    val notifResult = notificationService.notifyTutorOfPaidQuestion(
                        tutorId = question.targetTutorId,
                        questionId = docRef.id,
                        domain = question.domain,
                        studentId = question.studentId
                    )
                    if (notifResult.isSuccess) {
                        Log.d(TAG, "✓ Successfully sent paid question notification to tutor: ${question.targetTutorId}")
                    } else {
                        Log.e(TAG, "✗ Failed to send paid question notification: ${notifResult.exceptionOrNull()?.message}")
                    }
                } else if (question.type == "written" && question.visibility == "public") {
                    // Free question - notify all tutors with matching domain
                    Log.d(TAG, "Searching for tutors in domain: ${question.domain}")

                    // Get tutors who teach this domain
                    val tutorsSnapshot = firestore.collection("users")
                        .whereEqualTo("role", "tutor")
                        .whereArrayContains("domains", question.domain)
                        .get()
                        .await()

                    Log.d(TAG, "Found ${tutorsSnapshot.size()} tutors for domain ${question.domain}")

                    if (tutorsSnapshot.isEmpty) {
                        Log.w(TAG, "No tutors found for domain: ${question.domain}")
                    }

                    var successCount = 0
                    var failCount = 0

                    tutorsSnapshot.documents.forEach { tutorDoc ->
                        val tutorId = tutorDoc.id
                        Log.d(TAG, "Sending notification to tutor: $tutorId")
                        val notifResult = notificationService.notifyTutorOfFreeQuestion(
                            tutorId = tutorId,
                            questionId = docRef.id,
                            domain = question.domain,
                            studentId = question.studentId
                        )
                        if (notifResult.isSuccess) {
                            successCount++
                        } else {
                            failCount++
                            Log.e(TAG, "Failed to notify tutor $tutorId: ${notifResult.exceptionOrNull()?.message}")
                        }
                    }
                    Log.d(TAG, "Notification results: $successCount successful, $failCount failed out of ${tutorsSnapshot.size()} tutors")
                }
            } catch (notifEx: Exception) {
                Log.e(TAG, "Exception while sending question notification: ${notifEx.message}", notifEx)
                // Don't fail the whole operation if notification fails
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(studentId: String, imageUri: Uri): Result<String> {
        return try {
            val path = "question_images/$studentId/${System.currentTimeMillis()}.jpg"
            val ref = storage.reference.child(path)
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuestions(): Result<List<QuestionPost>> {
        return try {
            val snapshot = firestore.collection("questions").get().await()
            val questions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(QuestionPost::class.java)?.copy(id = doc.id)
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOpenQuestionsByType(type: String): Result<List<QuestionPost>> {
        return try {
            val snapshot = firestore.collection("questions")
                .whereEqualTo("type", type)
                .whereEqualTo("status", "open")
                .get()
                .await()
            val questions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(QuestionPost::class.java)?.copy(id = doc.id)
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadSolutionFile(tutorId: String, questionId: String, uri: Uri, isVideo: Boolean): Result<String> {
        return try {
            val kind = if (isVideo) "video" else "image"
            val path = "solutions/$questionId/$tutorId/${kind}_${System.currentTimeMillis()}"
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postSolution(questionId: String, solution: Solution): Result<Boolean> {
        return try {
            val colRef = firestore.collection("questions").document(questionId).collection("solutions")
            val docRef = colRef.document()
            val withId = solution.copy(id = docRef.id, timestamp = Date())
            docRef.set(withId).await()

            // Send notification to student
            try {
                Log.d(TAG, "Attempting to send solution notification for question: $questionId")
                // Get the question to find the student and domain
                val questionDoc = firestore.collection("questions").document(questionId).get().await()
                if (questionDoc.exists()) {
                    val studentId = questionDoc.getString("studentId") ?: ""
                    val domain = questionDoc.getString("domain") ?: questionDoc.getString("subjectCategory") ?: "your subject"

                    Log.d(TAG, "Found question details - studentId: $studentId, domain: $domain")

                    if (studentId.isNotBlank()) {
                        val notifResult = notificationService.notifyStudentOfSolution(
                            studentId = studentId,
                            questionId = questionId,
                            tutorId = solution.tutorId,
                            domain = domain
                        )
                        if (notifResult.isSuccess) {
                            Log.d(TAG, "✓ Successfully sent solution notification to student: $studentId")
                        } else {
                            Log.e(TAG, "✗ Failed to send solution notification: ${notifResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.w(TAG, "Student ID is blank, cannot send notification")
                    }
                } else {
                    Log.w(TAG, "Question document does not exist: $questionId")
                }
            } catch (notifEx: Exception) {
                Log.e(TAG, "Exception while sending solution notification: ${notifEx.message}", notifEx)
                // Don't fail the whole operation if notification fails
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOpenQuestionsForStudent(studentId: String): Result<List<QuestionPost>> {
        return try {
            val snapshot = firestore.collection("questions")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "open")
                .get()
                .await()
            val questions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(QuestionPost::class.java)?.copy(id = doc.id)
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSolutionsForQuestion(questionId: String): Result<List<Solution>> {
        return try {
            val snapshot = firestore.collection("questions")
                .document(questionId)
                .collection("solutions")
                .orderBy("timestamp")
                .get()
                .await()
            val solutions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Solution::class.java)?.copy(id = doc.id)
            }
            Result.success(solutions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuestionsForStudent(studentId: String): Result<List<QuestionPost>> {
        return try {
            val snapshot = firestore.collection("questions")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(QuestionPost::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.timestamp }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rateSolution(questionId: String, solutionId: String, rating: Double, raterId: String): Result<Boolean> {
        return try {
            val docRef = firestore.collection("questions").document(questionId)
                .collection("solutions").document(solutionId)
            val data = mapOf(
                "rating" to rating,
                "ratedBy" to raterId,
                "ratedAt" to Date()
            )
            docRef.update(data).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuestionAndSolutions(questionId: String): Result<Boolean> {
        return try {
            val questionRef = firestore.collection("questions").document(questionId)
            val questionSnap = questionRef.get().await()
            val studentId = questionSnap.getString("studentId") ?: ""

            val solutionsRef = questionRef.collection("solutions")
            val snapshot = solutionsRef.get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.delete(questionRef)
            if (studentId.isNotBlank()) {
                val userRef = firestore.collection("users").document(studentId)
                batch.set(userRef, mapOf("totalSolvedQuestions" to FieldValue.increment(1)), SetOptions.merge())
            }
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
