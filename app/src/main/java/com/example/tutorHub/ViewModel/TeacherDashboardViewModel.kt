package com.example.tutorHub.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.TeacherDashboardData
import com.example.tutorHub.data.repository.DashboardRepository
import kotlinx.coroutines.launch

class TeacherDashboardViewModel(private val repository: DashboardRepository = DashboardRepository()) : ViewModel() {
    private val _dashboardData = MutableLiveData<TeacherDashboardData?>()
    val dashboardData: MutableLiveData<TeacherDashboardData?> = _dashboardData

    fun fetchDashboard(teacherId: String) {
        viewModelScope.launch {
            val result = repository.getTeacherDashboardData(teacherId)
            if (result.isSuccess) {
                _dashboardData.value = result.getOrNull()
            }
        }
    }
}

