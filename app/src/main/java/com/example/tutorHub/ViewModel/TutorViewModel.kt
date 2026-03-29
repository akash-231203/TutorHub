package com.example.tutorHub.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.Tutor
import com.example.tutorHub.data.repository.TutorRepository
import kotlinx.coroutines.launch

class TutorViewModel(private val repository: TutorRepository = TutorRepository()) : ViewModel() {
    private val _domains = MutableLiveData<List<String>>(emptyList())
    val domains: LiveData<List<String>> = _domains

    private val _tutors = MutableLiveData<List<Tutor>>(emptyList())
    val tutors: LiveData<List<Tutor>> = _tutors

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun fetchTutorsByDomain(domain: String) {
        viewModelScope.launch {
            val result = repository.getTutorsByDomain(domain)
            if (result.isSuccess) {
                _tutors.value = result.getOrNull().orEmpty()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun clearError() { _error.value = null }
}

