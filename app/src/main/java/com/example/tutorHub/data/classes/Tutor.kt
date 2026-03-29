package com.example.tutorHub.data.classes

data class Tutor(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val domains: List<String> = emptyList(),
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0
)
