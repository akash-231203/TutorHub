package com.example.tutorHub.util

import java.util.Locale

/**
 * Formats rating as either:
 * - "No ratings yet" when count is 0 or less
 * - "X.X/5 (N)" otherwise, using current Locale for decimal formatting
 */
fun formatRating(average: Double, count: Int): String {
    return if (count <= 0) {
        "No ratings yet"
    } else {
        String.format(Locale.getDefault(), "%.1f/5 (%d)", average, count)
    }
}
