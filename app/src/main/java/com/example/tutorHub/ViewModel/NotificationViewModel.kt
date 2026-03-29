package com.example.tutorHub.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutorHub.data.classes.Notification
import com.example.tutorHub.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val _notifications = MutableLiveData<List<Notification>>(emptyList())
    val notifications: LiveData<List<Notification>> = _notifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadNotifications(userId: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = repository.getNotificationsForUser(userId)
            _loading.value = false
            if (result.isSuccess) {
                _notifications.value = result.getOrNull().orEmpty()
                updateUnreadCount()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun loadUnreadCount(userId: String) {
        viewModelScope.launch {
            val result = repository.getUnreadNotificationCount(userId)
            if (result.isSuccess) {
                _unreadCount.value = result.getOrNull() ?: 0
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val result = repository.markNotificationAsRead(notificationId)
            if (result.isSuccess) {
                updateUnreadCount()
                // Refresh notifications list to update read status
                val currentNotifications = _notifications.value.orEmpty()
                _notifications.value = currentNotifications.map {
                    if (it.id == notificationId) it.copy(read = true) else it
                }
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            val result = repository.markAllNotificationsAsRead(userId)
            if (result.isSuccess) {
                _unreadCount.value = 0
                // Update all notifications to read
                val currentNotifications = _notifications.value.orEmpty()
                _notifications.value = currentNotifications.map { it.copy(read = true) }
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val result = repository.deleteNotification(notificationId)
            if (result.isSuccess) {
                updateUnreadCount()
                // Remove from list
                val currentNotifications = _notifications.value.orEmpty()
                _notifications.value = currentNotifications.filter { it.id != notificationId }
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun deleteOldNotifications(userId: String, daysOld: Int = 30) {
        viewModelScope.launch {
            val result = repository.deleteOldNotifications(userId, daysOld)
            if (result.isSuccess) {
                // Refresh notifications after deletion
                loadNotifications(userId)
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    private fun updateUnreadCount() {
        val unread = _notifications.value.orEmpty().count { !it.read }
        _unreadCount.value = unread
    }

    fun clearError() {
        _error.value = null
    }
}

