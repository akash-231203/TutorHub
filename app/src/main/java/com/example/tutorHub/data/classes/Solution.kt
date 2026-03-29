package com.example.tutorHub.data.classes

import java.util.Date

data class Solution(
    val id: String = "",
    val questionId: String = "",
    val tutorId: String = "",
    val description: String = "",
    val attachmentUrl: String = "",
    val attachmentType: String = "", // "image" or "video"
    val timestamp: Date = Date(),
    val rating: Double? = null,
    val ratedBy: String? = null,
    val ratedAt: Date? = null
)
