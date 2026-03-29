package com.example.tutorHub.data.classes

import java.util.Date

data class Review(
    val id: String = "",
    val sessionId: String = "",
    val studentId: String = "",
    val teacherId: String = "",
    val rating: Int = 0,
    val reviewText: String = "",
    val helpfulness: Int = 0,
    val clarity: Int = 0,
    val responsiveness: Int = 0,
    val timestamp: Date = Date()
)

