package com.example.tutorHub.data.classes

import java.util.Date

// Data class for a question/topic post
// status: "open", "answered", "closed"
data class QuestionPost(
    val id: String = "",
    val studentId: String = "",
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val subjectCategory: String = "",
    val timestamp: Date = Date(),
    val status: String = "open",
    // Added fields to support new flows
    val imageUrl: String? = null,               // for uploaded photo (written solution)
    val type: String = "written",              // "written" or "video"
    val visibility: String = "public",         // "public" or "private"
    val targetTutorId: String? = null,          // set when visibility is private
    val domain: String = ""                    // selected domain (for video explanation)
)
