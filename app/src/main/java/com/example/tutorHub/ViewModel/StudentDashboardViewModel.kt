package com.example.tutorHub.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.StudentDashboardData
import com.example.tutorHub.data.repository.DashboardRepository
import kotlinx.coroutines.launch

class StudentDashboardViewModel(private val repository: DashboardRepository = DashboardRepository()) : ViewModel() {
    private val _dashboardData = MutableLiveData<StudentDashboardData?>()
    val dashboardData: MutableLiveData<StudentDashboardData?> = _dashboardData

    fun fetchDashboard(studentId: String) {
        viewModelScope.launch {
            val result = repository.getStudentDashboardData(studentId)
            if (result.isSuccess) {
                _dashboardData.value = result.getOrNull()
            }
        }
    }
}

