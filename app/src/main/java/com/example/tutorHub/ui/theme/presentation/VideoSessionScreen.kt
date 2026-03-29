package com.example.tutorHub.ui.theme.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

private const val TAG = "VideoSessionScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSessionScreen(
    navController: NavController,
    sessionId: String,
    currentUserId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sessionData by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var tutorName by remember { mutableStateOf("Tutor") }
    var studentName by remember { mutableStateOf("Student") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoCallStarted by remember { mutableStateOf(false) }

    // Timer state
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var sessionActive by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    // Load session data with real-time updates
    DisposableEffect(sessionId) {
        Log.d(TAG, "Setting up session listener for: $sessionId")
        
        val listenerRegistration = firestore.collection("sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to session: ${error.message}", error)
                    errorMessage = error.message
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    errorMessage = "Session not found"
                    isLoading = false
                    return@addSnapshotListener
                }
                
                try {
                    val status = snapshot.getString("status")
                    val videoSessionStartedAt = snapshot.getDate("videoSessionStartedAt")
                    val durationMinutes = snapshot.getLong("durationMinutes")?.toInt() ?: 60
                    val existingRoomId = snapshot.getString("videoRoomId")
                    val rescheduleProposedTime = snapshot.getDate("rescheduleProposedTime")
                    val requestedTime = snapshot.getDate("requestedTime")
                    
                    // Use rescheduleProposedTime if available, otherwise requestedTime
                    val scheduledTime = rescheduleProposedTime ?: requestedTime
                    
                    // Generate a simpler, shorter room ID without special prefixes
                    val roomId = existingRoomId ?: sessionId.replace("-", "").take(16)
                    
                    sessionData = mapOf(
                        "studentId" to snapshot.getString("studentId"),
                        "teacherId" to snapshot.getString("teacherId"),
                        "domain" to snapshot.getString("domain"),
                        "requestedTime" to requestedTime,
                        "rescheduleProposedTime" to rescheduleProposedTime,
                        "scheduledTime" to scheduledTime,
                        "durationMinutes" to durationMinutes,
                        "status" to status,
                        "videoRoomId" to roomId,
                        "videoSessionStartedAt" to videoSessionStartedAt
                    )

                    val studentId = snapshot.getString("studentId") ?: ""
                    val teacherId = snapshot.getString("teacherId") ?: ""

                    // Fetch user names on first load
                    if (tutorName == "Tutor" || studentName == "Student") {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                if (studentId.isNotEmpty()) {
                                    val studentDoc = firestore.collection("users").document(studentId).get().await()
                                    studentName = studentDoc.getString("name") ?: "Student"
                                }

                                if (teacherId.isNotEmpty()) {
                                    val tutorDoc = firestore.collection("users").document(teacherId).get().await()
                                    tutorName = tutorDoc.getString("name") ?: "Tutor"
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error fetching user names: ${e.message}", e)
                            }
                        }
                    }

                    // Calculate session end time based on scheduled time
                    if (scheduledTime != null) {
                        val scheduledTimeMs = scheduledTime.time
                        val sessionEndTimeMs = scheduledTimeMs + (durationMinutes * 60 * 1000L)
                        val now = System.currentTimeMillis()
                        
                        // Calculate elapsed time from scheduled start
                        val elapsedMs = now - scheduledTimeMs
                        elapsedSeconds = (elapsedMs / 1000).coerceAtLeast(0)
                        
                        // Check if session should be auto-completed
                        if (now >= sessionEndTimeMs && (status == "confirmed" || status == "in_progress")) {
                            Log.d(TAG, "Session has exceeded scheduled end time, auto-completing")
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    firestore.collection("sessions").document(sessionId).update(
                                        mapOf(
                                            "status" to "completed",
                                            "videoSessionEndedAt" to Date(),
                                            "updatedAt" to Date()
                                        )
                                    ).await()
                                    Log.d(TAG, "Expired session marked as completed")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error auto-ending expired session: ${e.message}", e)
                                }
                            }
                            errorMessage = "This session has expired"
                            isLoading = false
                            return@addSnapshotListener
                        }
                    }

                    // Handle session status
                    when (status) {
                        "completed" -> {
                            errorMessage = "This session has already ended"
                            isLoading = false
                        }
                        "in_progress", "confirmed" -> {
                            isLoading = false
                            sessionActive = true
                            Log.d(TAG, "Session ready (status: $status). Elapsed: $elapsedSeconds sec")
                            
                            // If confirmed and someone is joining, mark as in_progress
                            if (status == "confirmed" && !videoCallStarted) {
                                videoCallStarted = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        Log.d(TAG, "Marking session as in_progress")
                                        val updates = mutableMapOf<String, Any>(
                                            "status" to "in_progress",
                                            "videoSessionStartedAt" to Date(),
                                            "updatedAt" to Date()
                                        )
                                        
                                        if (existingRoomId == null) {
                                            updates["videoRoomId"] = roomId
                                        }
                                        
                                        firestore.collection("sessions").document(sessionId).update(updates).await()
                                        Log.d(TAG, "Session status updated to in_progress")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error updating session: ${e.message}", e)
                                    }
                                }
                            }
                        }
                        else -> {
                            errorMessage = "Session is not ready to join (status: $status)"
                            isLoading = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing session update: ${e.message}", e)
                    errorMessage = e.message
                    isLoading = false
                }
            }
        
        onDispose {
            Log.d(TAG, "Removing session listener")
            listenerRegistration.remove()
        }
    }

    // Continuous timer based on scheduled time (not when user joined)
    LaunchedEffect(sessionData["scheduledTime"]) {
        val scheduledTime = sessionData["scheduledTime"] as? Date
        val durationMinutes = (sessionData["durationMinutes"] as? Int) ?: 60
        val status = sessionData["status"] as? String
        
        if (scheduledTime != null && (status == "confirmed" || status == "in_progress")) {
            while (true) {
                val now = System.currentTimeMillis()
                val scheduledMs = scheduledTime.time
                val sessionEndMs = scheduledMs + (durationMinutes * 60 * 1000L)
                
                // Calculate elapsed from scheduled start time
                val elapsedMs = now - scheduledMs
                elapsedSeconds = (elapsedMs / 1000).coerceAtLeast(0)
                
                // Check if session end time has passed
                if (now >= sessionEndMs) {
                    Log.d(TAG, "Session time expired, auto-completing")
                    endSession(firestore, sessionId, navController)
                    break
                }
                
                delay(1000L)
            }
        }
    }

    // Check session duration
    val durationMinutes = (sessionData["durationMinutes"] as? Int) ?: 60
    val maxSeconds = durationMinutes * 60L
    val remainingSeconds = maxSeconds - elapsedSeconds

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading session...")
            }
        }
        return
    }

    if (errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "An error occurred",
                    textAlign = TextAlign.Center,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Video call UI - WebRTC implementation pending
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Video Session Active",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            val domain = sessionData["domain"] as? String ?: "Tutoring Session"
            Text(
                text = domain,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Timer display
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2C2C2E)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (remainingSeconds <= 300) Color(0xFFFF5722) else Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Time remaining: ${formatTime(remainingSeconds.coerceAtLeast(0))}",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Manual join button - WebRTC implementation coming soon
            Button(
                onClick = {
                    Log.d(TAG, "Join button clicked - WebRTC implementation pending")
                    errorMessage = "Video calling feature is being updated with WebRTC. Please check back soon!"
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (videoCallStarted) "Re-join Video Call" else "Join Video Call",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Video calling feature will be available soon with WebRTC",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

private fun endSession(
    firestore: FirebaseFirestore,
    sessionId: String,
    navController: NavController
) {
    firestore.collection("sessions").document(sessionId).update(
        mapOf(
            "status" to "completed",
            "videoSessionEndedAt" to Date(),
            "updatedAt" to Date()
        )
    ).addOnSuccessListener {
        Log.d(TAG, "Session ended successfully")
        navController.popBackStack()
    }.addOnFailureListener { e ->
        Log.e(TAG, "Error ending session: ${e.message}", e)
        navController.popBackStack()
    }
}
