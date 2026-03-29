package com.example.tutorHub.ui.theme.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * A composable that displays a countdown timer and a Join button for video sessions.
 * The button is disabled until the session time is reached.
 *
 * @param scheduledTime The scheduled start time of the session
 * @param durationMinutes Duration of the session in minutes
 * @param onJoinClick Callback when the Join button is clicked
 * @param sessionStatus Current status of the session
 */
@Composable
fun SessionJoinButton(
    scheduledTime: Date?,
    durationMinutes: Int,
    onJoinClick: () -> Unit,
    sessionStatus: String,
    modifier: Modifier = Modifier
) {
    // Only show for confirmed sessions, hide if already in_progress or completed
    if (scheduledTime == null || (sessionStatus != "confirmed" && sessionStatus != "in_progress")) {
        return
    }

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val scheduledTimeMillis = scheduledTime.time
    val sessionEndTimeMillis = scheduledTimeMillis + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())

    // Update current time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeUntilStart = scheduledTimeMillis - currentTime
    val timeUntilEnd = sessionEndTimeMillis - currentTime

    // Session states
    val isSessionTime = currentTime >= scheduledTimeMillis && currentTime < sessionEndTimeMillis
    val isSessionExpired = currentTime >= sessionEndTimeMillis
    val isWaiting = currentTime < scheduledTimeMillis

    // Allow joining 5 minutes early, but not if session is already in_progress
    val earlyJoinAllowed = timeUntilStart <= TimeUnit.MINUTES.toMillis(5) && timeUntilStart > 0 && sessionStatus == "confirmed"
    val canJoin = (isSessionTime || earlyJoinAllowed) && sessionStatus != "in_progress"

    // Show "Session in Progress" with JOIN button if status is in_progress
    if (sessionStatus == "in_progress") {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Session In Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
                Text(
                    text = "The video session has already started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                // Add Join button
                Button(
                    onClick = onJoinClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join Video Call",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    // Animated button color
    val buttonColor by animateColorAsState(
        targetValue = when {
            canJoin -> Color(0xFF4CAF50) // Green when can join
            isSessionExpired -> Color(0xFF9E9E9E) // Gray when expired
            else -> Color(0xFF2196F3) // Blue when waiting
        },
        label = "buttonColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canJoin) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (canJoin) Icons.Default.Videocam else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = buttonColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSessionExpired -> "Session Ended"
                        canJoin -> "Session is Ready!"
                        else -> "Session Scheduled"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = buttonColor
                )
            }

            // Timer display
            when {
                isSessionExpired -> {
                    Text(
                        text = "This session has ended",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
                canJoin -> {
                    // Show remaining session time
                    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilEnd)
                    val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilEnd) % 60

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (earlyJoinAllowed && !isSessionTime) {
                            Text(
                                text = "You can join early!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF388E3C)
                            )
                        }
                        Text(
                            text = "Time remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", remainingMinutes, remainingSeconds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingMinutes < 5) Color(0xFFFF5722) else Color(0xFF388E3C)
                        )
                    }
                }
                else -> {
                    // Show countdown to session start
                    CountdownTimer(timeUntilStart = timeUntilStart)
                }
            }

            // Join button
            Button(
                onClick = onJoinClick,
                enabled = canJoin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = Color(0xFFBDBDBD)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSessionExpired -> "Session Ended"
                        canJoin -> "Join Video Session"
                        else -> "Waiting for Session Time"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Info text
            if (isWaiting && !earlyJoinAllowed) {
                Text(
                    text = "You can join 5 minutes before the scheduled time",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CountdownTimer(timeUntilStart: Long) {
    val days = TimeUnit.MILLISECONDS.toDays(timeUntilStart)
    val hours = TimeUnit.MILLISECONDS.toHours(timeUntilStart) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilStart) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilStart) % 60

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Session starts in",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (days > 0) {
                TimeUnit(value = days.toInt(), label = "DAYS")
            }
            TimeUnit(value = hours.toInt(), label = "HRS")
            Text(":", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF2196F3))
            TimeUnit(value = minutes.toInt(), label = "MIN")
            Text(":", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF2196F3))
            TimeUnit(value = seconds.toInt(), label = "SEC")
        }
    }
}

@Composable
private fun TimeUnit(value: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2196F3),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
