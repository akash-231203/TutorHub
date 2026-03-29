package com.example.tutorHub.ui.theme.presentation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController
import com.example.tutorHub.ViewModel.SessionViewModel
import com.example.tutorHub.data.classes.SessionRequest
import com.example.tutorHub.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRequestsScreen(
    navController: NavController,
    teacherId: String,
    viewModel: SessionViewModel = viewModel<SessionViewModel>()
) {
    val requests by viewModel.teacherRequests.observeAsState(emptyList())
    val serverError by viewModel.error.observeAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(teacherId) {
        Log.d("TeacherRequestsScreen", "Loading teacher requests for: $teacherId")
        viewModel.loadTeacherRequests(teacherId)
    }

    // Sort requests by newest created_at first
    val sortedRequests = requests.sortedByDescending { it.createdAt ?: Date(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Session Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Message notifications area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Server error
                if (!serverError.isNullOrEmpty()) {
                    MessageCard(
                        message = serverError ?: "",
                        type = TeacherMessageType.ERROR,
                        onDismiss = { /* Server errors can't be dismissed manually */ }
                    )
                }

                // Local error
                errorMessage?.let { msg ->
                    MessageCard(
                        message = msg,
                        type = TeacherMessageType.ERROR,
                        onDismiss = { errorMessage = null }
                    )
                }

                // Success message
                successMessage?.let { msg ->
                    MessageCard(
                        message = msg,
                        type = TeacherMessageType.SUCCESS,
                        onDismiss = { successMessage = null }
                    )
                }
            }

            // Content area
            if (sortedRequests.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = sortedRequests, key = { it.id }) { req: SessionRequest ->
                        TeacherRequestCard(
                            request = req,
                            sdf = sdf,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

private enum class TeacherMessageType {
    SUCCESS, ERROR
}

@Composable
private fun MessageCard(
    message: String,
    type: TeacherMessageType,
    onDismiss: () -> Unit
) {
    val (backgroundColor, iconColor, icon) = when (type) {
        TeacherMessageType.SUCCESS -> Triple(
            SurfaceGreen,
            SuccessGreen,
            Icons.Default.CheckCircle
        )
        TeacherMessageType.ERROR -> Triple(
            SurfaceRed,
            ErrorRed,
            Icons.Default.Error
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = iconColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                color = SurfaceBlue,
                shape = RoundedCornerShape(60.dp)
            ) {
                Icon(
                    Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.padding(30.dp),
                    tint = PrimaryBlue
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "No Session Requests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You don't have any pending session requests.\nRequests from students will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun TeacherRequestCard(
    request: SessionRequest,
    sdf: SimpleDateFormat,
    navController: NavController
) {
    var studentName by remember { mutableStateOf<String?>(null) }
    var isLoadingName by remember { mutableStateOf(true) }

    LaunchedEffect(request.studentId) {
        isLoadingName = true
        try {
            Log.d("TeacherRequestCard", "Fetching student name for ID: ${request.studentId}")
            val studentDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(request.studentId)
                .get()
                .await()

            if (!studentDoc.exists()) {
                Log.w("TeacherRequestCard", "Student document does not exist for ID: ${request.studentId}")
                studentName = "Unknown Student"
            } else {
                val name = studentDoc.getString("name")
                Log.d("TeacherRequestCard", "Fetched name: $name for student ID: ${request.studentId}")

                studentName = if (!name.isNullOrBlank()) {
                    name
                } else {
                    Log.w("TeacherRequestCard", "Name field is null or blank for student ID: ${request.studentId}")
                    "Unknown Student"
                }
            }
        } catch (e: Exception) {
            Log.e("TeacherRequestCard", "Error fetching student name for ${request.studentId}: ${e.message}", e)
            studentName = "Unknown Student"
        } finally {
            isLoadingName = false
        }
    }

    val statusInfo = getStatusInfo(request)

    // Check if session can be joined
    val currentTime = System.currentTimeMillis()
    val scheduledTime = (request.rescheduleProposedTime ?: request.requestedTime)?.time ?: 0L
    val sessionEndTime = scheduledTime + (request.durationMinutes * 60 * 1000L)
    val canJoin = request.status == "confirmed" && currentTime >= scheduledTime && currentTime < sessionEndTime
    val earlyJoinAllowed = request.status == "confirmed" &&
                          (scheduledTime - currentTime) <= (5 * 60 * 1000L) &&
                          (scheduledTime - currentTime) > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("teacher_request_details/${request.id}")
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = statusInfo.color
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        color = statusInfo.iconBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = statusInfo.icon,
                            contentDescription = request.status,
                            modifier = Modifier.padding(14.dp),
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            statusInfo.text,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusInfo.iconBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            statusInfo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View Details",
                        tint = TextSecondary
                    )
                }
            }

            // Main Content
            Column(modifier = Modifier.padding(20.dp)) {
                // Session Details Section
                Text(
                    "SESSION DETAILS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Student",
                    value = when {
                        isLoadingName -> "Loading..."
                        studentName != null -> studentName!!
                        else -> "Loading..."
                    },
                    color = PrimaryBlue
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    icon = Icons.Default.School,
                    label = "Subject",
                    value = request.domain,
                    color = Color(0xFF9C27B0)
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = "${request.durationMinutes} minutes",
                    color = AccentOrange
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    icon = Icons.Default.Event,
                    label = "Scheduled Time",
                    value = (request.rescheduleProposedTime ?: request.requestedTime)?.let { sdf.format(it) } ?: "-",
                    color = SuccessGreen
                )

                // Join Button (visible when session time is reached)
                if (canJoin || earlyJoinAllowed) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            navController.navigate("video_session/${request.id}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canJoin) SuccessGreen else PrimaryBlue
                        )
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Join",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (canJoin) "Join Session Now" else "Join Early (5 min before)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Tap for more details hint
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tap card for full details and actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private data class StatusInfo(
    val color: Color,
    val iconBackground: Color,
    val icon: ImageVector,
    val text: String,
    val description: String
)

private fun getStatusInfo(request: SessionRequest): StatusInfo {
    return when (request.status) {
        "pending", "requested" -> StatusInfo(
            color = SurfaceBlue,
            iconBackground = PrimaryBlue,
            icon = Icons.Default.HourglassEmpty,
            text = "Pending Review",
            description = "Student is waiting for your response"
        )
        "reschedule_pending" -> StatusInfo(
            color = SurfaceOrange,
            iconBackground = AccentOrange,
            icon = Icons.Default.Schedule,
            text = "Reschedule Proposed",
            description = if (request.rescheduleProposedBy == "student") "Student proposed new time" else "You proposed new time"
        )
        "confirmed" -> StatusInfo(
            color = SurfaceGreen,
            iconBackground = SuccessGreen,
            icon = Icons.Default.CheckCircle,
            text = "Session Confirmed",
            description = "Session is scheduled"
        )
        "in_progress" -> StatusInfo(
            color = SurfaceGreen,
            iconBackground = Color(0xFF4CAF50),
            icon = Icons.Default.Videocam,
            text = "In Progress",
            description = "Session is currently active"
        )
        "completed" -> StatusInfo(
            color = SurfaceBlue,
            iconBackground = PrimaryBlue,
            icon = Icons.Default.Done,
            text = "Completed",
            description = "Session has ended"
        )
        "rejected" -> StatusInfo(
            color = SurfaceRed,
            iconBackground = ErrorRed,
            icon = Icons.Default.Cancel,
            text = "Rejected",
            description = if (request.rejectedBy == "teacher") "You declined" else "Student cancelled"
        )
        else -> StatusInfo(
            color = BackgroundLight,
            iconBackground = Color.Gray,
            icon = Icons.Default.Info,
            text = request.status.uppercase(),
            description = "Status unknown"
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.padding(10.dp),
                tint = color
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
