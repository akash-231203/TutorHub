package com.example.tutorHub.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.SessionRequest
import kotlinx.coroutines.launch
import java.util.Date

class SessionViewModel(
    private val repository: com.example.tutorHub.data.repository.SessionRepository = com.example.tutorHub.data.repository.SessionRepository()
) : ViewModel() {
    private val _studentRequests = MutableLiveData<List<SessionRequest>>(emptyList())
    val studentRequests: LiveData<List<SessionRequest>> = _studentRequests

    private val _teacherRequests = MutableLiveData<List<SessionRequest>>(emptyList())
    val teacherRequests: LiveData<List<SessionRequest>> = _teacherRequests

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun createRequest(request: SessionRequest, onResult: (Result<String>) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.createRequest(request)
            _loading.value = false
            if (res.isSuccess) onResult(Result.success(res.getOrNull()!!))
            else onResult(Result.failure(res.exceptionOrNull()!!))
        }
    }

    fun loadStudentRequests(studentId: String) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.getRequestsForStudent(studentId)
            _loading.value = false
            if (res.isSuccess) _studentRequests.value = res.getOrNull().orEmpty()
            else _error.value = res.exceptionOrNull()?.message
        }
    }

    fun loadTeacherRequests(teacherId: String) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.getRequestsForTeacher(teacherId)
            _loading.value = false
            if (res.isSuccess) _teacherRequests.value = res.getOrNull().orEmpty()
            else _error.value = res.exceptionOrNull()?.message
        }
    }

    fun acceptRequest(request: SessionRequest, onResult: (Result<String>) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.confirmRequest(request)
            _loading.value = false
            if (res.isSuccess) onResult(Result.success(res.getOrNull()!!))
            else onResult(Result.failure(res.exceptionOrNull()!!))
        }
    }

    fun rejectRequest(requestId: String, onResult: (Result<Boolean>) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.rejectRequest(requestId, null)
            _loading.value = false
            if (res.isSuccess) onResult(Result.success(true))
            else onResult(Result.failure(res.exceptionOrNull()!!))
        }
    }

    fun rejectRequestBy(requestId: String, rejectedBy: String, onResult: (Result<Boolean>) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.rejectRequest(requestId, rejectedBy)
            _loading.value = false
            if (res.isSuccess) onResult(Result.success(true))
            else onResult(Result.failure(res.exceptionOrNull()!!))
        }
    }

    fun proposeReschedule(requestId: String, newTime: Date, proposedBy: String, onResult: (Result<Boolean>) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            val res = repository.proposeReschedule(requestId, newTime, proposedBy)
            _loading.value = false
            if (res.isSuccess) onResult(Result.success(true))
            else onResult(Result.failure(res.exceptionOrNull()!!))
        }
    }

    fun clearError() { _error.value = null }
}
