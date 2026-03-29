package com.example.tutorHub.data.classes

data class StudentDashboardData(
    val postedQuestions: Int = 0,
    val sessionsAttended: Int = 0,
    val queriesPending: Int = 0,
    val reviewsGiven: Int = 0,
    val solvedQuestions: Int = 0
)