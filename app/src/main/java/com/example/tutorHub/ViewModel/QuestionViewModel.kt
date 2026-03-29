package com.example.tutorHub.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.QuestionPost
import com.example.tutorHub.data.classes.Solution
import com.example.tutorHub.data.repository.QuestionRepository
import com.example.tutorHub.data.repository.TutorRepository
import kotlinx.coroutines.launch
import java.util.Date

class QuestionViewModel(
    private val repository: QuestionRepository = QuestionRepository(),
    private val tutorRepo: TutorRepository = TutorRepository()
) : ViewModel() {
    private val _postStatus = MutableLiveData<Result<Boolean>?>(null)
    val postStatus: LiveData<Result<Boolean>?> = _postStatus

    private val _questions = MutableLiveData<List<QuestionPost>>(emptyList())
    val questions: LiveData<List<QuestionPost>> = _questions

    private val _uploadResult = MutableLiveData<Result<String>?>(null)
    val uploadResult: LiveData<Result<String>?> = _uploadResult

    private val _isPosting = MutableLiveData(false)
    val isPosting: LiveData<Boolean> = _isPosting

    private val _unpaid = MutableLiveData<List<QuestionPost>>(emptyList())
    val unpaid: LiveData<List<QuestionPost>> = _unpaid

    private val _paid = MutableLiveData<List<QuestionPost>>(emptyList())
    val paid: LiveData<List<QuestionPost>> = _paid

    private val _solutionStatus = MutableLiveData<Result<Boolean>?>(null)
    val solutionStatus: LiveData<Result<Boolean>?> = _solutionStatus

    private val _studentOpen = MutableLiveData<List<QuestionPost>>(emptyList())
    val studentOpen: LiveData<List<QuestionPost>> = _studentOpen

    private val _solutions = MutableLiveData<List<Solution>>(emptyList())
    val solutions: LiveData<List<Solution>> = _solutions

    private val _solutionsQuestionId = MutableLiveData<String?>(null)
    val solutionsQuestionId: LiveData<String?> = _solutionsQuestionId

    private val _isLoadingSolutions = MutableLiveData(false)
    val isLoadingSolutions: LiveData<Boolean> = _isLoadingSolutions

    private val _studentAll = MutableLiveData<List<QuestionPost>>(emptyList())
    val studentAll: LiveData<List<QuestionPost>> = _studentAll

    private val _ratingStatus = MutableLiveData<Result<Boolean>?>(null)
    val ratingStatus: LiveData<Result<Boolean>?> = _ratingStatus

    private val _deleteStatus = MutableLiveData<Result<Boolean>?>(null)
    val deleteStatus: LiveData<Result<Boolean>?> = _deleteStatus

    fun postQuestion(question: QuestionPost) {
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val result = repository.postQuestion(question)
                _postStatus.value = result
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun uploadImage(studentId: String, imageUri: Uri) {
        viewModelScope.launch {
            _uploadResult.value = repository.uploadImage(studentId, imageUri)
        }
    }

    fun fetchQuestions() {
        viewModelScope.launch {
            val result = repository.getQuestions()
            if (result.isSuccess) {
                _questions.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun loadTutorFeeds(tutorId: String, limit: Int = 3) {
        viewModelScope.launch {
            val domainsRes = tutorRepo.getTutorDomains(tutorId)
            if (domainsRes.isFailure) return@launch
            val domains = domainsRes.getOrNull().orEmpty()

            val writtenRes = repository.getOpenQuestionsByType("written")
            val videoRes = repository.getOpenQuestionsByType("video")

            val written = writtenRes.getOrNull().orEmpty()
                .filter { it.domain in domains && it.visibility == "public" }
                .take(limit)
            val video = videoRes.getOrNull().orEmpty()
                .filter { (it.targetTutorId == tutorId) || (it.visibility == "public" && it.domain in domains) }
                .take(limit)

            _unpaid.value = written
            _paid.value = video
        }
    }

    fun loadAllForTutor(tutorId: String, type: String) {
        viewModelScope.launch {
            val domainsRes = tutorRepo.getTutorDomains(tutorId)
            if (domainsRes.isFailure) return@launch
            val domains = domainsRes.getOrNull().orEmpty()

            val res = repository.getOpenQuestionsByType(type)
            val list = res.getOrNull().orEmpty().filter {
                if (type == "written") it.domain in domains && it.visibility == "public"
                else (it.targetTutorId == tutorId) || (it.visibility == "public" && it.domain in domains)
            }
            _questions.value = list
        }
    }

    fun clearPostStatus() { _postStatus.value = null }
    fun clearUploadResult() { _uploadResult.value = null }
    fun clearSolutionStatus() { _solutionStatus.value = null }
    fun clearRatingStatus() { _ratingStatus.value = null }
    fun clearDeleteStatus() { _deleteStatus.value = null }

    fun postWrittenWithImage(
        studentId: String,
        imageUri: Uri,
        description: String,
        domain: String
    ) {
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val uploadRes = repository.uploadImage(studentId, imageUri)
                if (uploadRes.isFailure) {
                    _postStatus.value = Result.failure(uploadRes.exceptionOrNull() ?: Exception("Upload failed"))
                    return@launch
                }
                val url = uploadRes.getOrNull() ?: run {
                    _postStatus.value = Result.failure(Exception("Upload URL missing"))
                    return@launch
                }
                val question = QuestionPost(
                    studentId = studentId,
                    title = "Written question",
                    description = description,
                    tags = emptyList(),
                    subjectCategory = domain,
                    timestamp = Date(),
                    status = "open",
                    imageUrl = url,
                    type = "written",
                    visibility = "public",
                    targetTutorId = null,
                    domain = domain
                )
                val postRes = repository.postQuestion(question)
                _postStatus.value = postRes
            } catch (e: Exception) {
                _postStatus.value = Result.failure(e)
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun postVideoWithImage(
        studentId: String,
        imageUri: Uri,
        description: String,
        domain: String,
        tutorId: String
    ) {
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val uploadRes = repository.uploadImage(studentId, imageUri)
                if (uploadRes.isFailure) {
                    _postStatus.value = Result.failure(uploadRes.exceptionOrNull() ?: Exception("Upload failed"))
                    return@launch
                }
                val url = uploadRes.getOrNull() ?: run {
                    _postStatus.value = Result.failure(Exception("Upload URL missing"))
                    return@launch
                }
                val question = QuestionPost(
                    studentId = studentId,
                    title = "Video explanation request",
                    description = description,
                    tags = emptyList(),
                    subjectCategory = domain,
                    timestamp = Date(),
                    status = "open",
                    imageUrl = url,
                    type = "video",
                    visibility = "private",
                    targetTutorId = tutorId,
                    domain = domain
                )
                val postRes = repository.postQuestion(question)
                _postStatus.value = postRes
            } catch (e: Exception) {
                _postStatus.value = Result.failure(e)
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun postSolutionForQuestion(
        questionId: String,
        tutorId: String,
        description: String,
        attachment: Uri,
        isVideo: Boolean
    ) {
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val urlRes = repository.uploadSolutionFile(tutorId, questionId, attachment, isVideo)
                if (urlRes.isFailure) {
                    _solutionStatus.value = Result.failure(urlRes.exceptionOrNull() ?: Exception("Upload failed"))
                    return@launch
                }
                val url = urlRes.getOrNull() ?: run {
                    _solutionStatus.value = Result.failure(Exception("Upload URL missing"))
                    return@launch
                }
                val solution = Solution(
                    questionId = questionId,
                    tutorId = tutorId,
                    description = description,
                    attachmentUrl = url,
                    attachmentType = if (isVideo) "video" else "image"
                )
                val res = repository.postSolution(questionId, solution)
                _solutionStatus.value = res
            } catch (e: Exception) {
                _solutionStatus.value = Result.failure(e)
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun loadOpenQuestionsForStudent(studentId: String, limit: Int? = null) {
        viewModelScope.launch {
            val res = repository.getOpenQuestionsForStudent(studentId)
            val list = res.getOrNull().orEmpty().let { l -> limit?.let { l.take(it) } ?: l }
            _studentOpen.value = list
        }
    }

    fun loadSolutionsForQuestion(questionId: String) {
        viewModelScope.launch {
            _isLoadingSolutions.value = true
            _solutionsQuestionId.value = questionId
            try {
                val res = repository.getSolutionsForQuestion(questionId)
                _solutions.value = res.getOrNull().orEmpty()
            } finally {
                _isLoadingSolutions.value = false
            }
        }
    }

    fun clearSolutions() {
        _solutions.value = emptyList()
        _solutionsQuestionId.value = null
    }

    fun fetchSolutionsForDecision(questionId: String, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val res = repository.getSolutionsForQuestion(questionId)
            val count = res.getOrNull()?.size ?: 0
            onDone(count)
        }
    }

    fun loadAllQuestionsForStudent(studentId: String) {
        viewModelScope.launch {
            val res = repository.getQuestionsForStudent(studentId)
            _studentAll.value = res.getOrNull().orEmpty()
        }
    }

    fun rateSolutionAndTutor(
        questionId: String,
        solutionId: String,
        tutorId: String,
        rating: Double,
        raterId: String
    ) {
        viewModelScope.launch {
            try {
                val sRes = repository.rateSolution(questionId, solutionId, rating, raterId)
                if (sRes.isFailure) { _ratingStatus.value = sRes; return@launch }
                val tRes = tutorRepo.addRatingToTutor(tutorId, rating)
                _ratingStatus.value = tRes
                // Refresh solutions list to reflect updated rating if needed
                loadSolutionsForQuestion(questionId)
            } catch (e: Exception) {
                _ratingStatus.value = Result.failure(e)
            }
        }
    }

    fun deleteQuestionWithSolutions(questionId: String) {
        viewModelScope.launch {
            try {
                val res = repository.deleteQuestionAndSolutions(questionId)
                _deleteStatus.value = res
            } catch (e: Exception) {
                _deleteStatus.value = Result.failure(e)
            }
        }
    }
}
