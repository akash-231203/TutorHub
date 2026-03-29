package com.example.tutorHub.data.classes

import java.util.Date

data class Session(
    val id: String = "",
    val questionId: String = "",
    val teacherId: String = "",
    val studentId: String = "",
    val scheduledTime: Date? = null,
    val durationMinutes: Int = 0,
    val status: String = "pending", // pending, scheduled, completed, cancelled
    val liveSessionLink: String = "",
    val fee: Double = 0.0
)
