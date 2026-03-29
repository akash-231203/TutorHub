package com.example.tutorHub.ui.theme.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tutorHub.ViewModel.SessionViewModel
import com.example.tutorHub.data.classes.SessionRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Color constants
private val PrimaryBlue = Color(0xFF2196F3)
private val AccentOrange = Color(0xFFFF9800)
private val SuccessGreen = Color(0xFF4CAF50)
private val ErrorRed = Color(0xFFF44336)
private val BackgroundGray = Color(0xFFF8F9FA)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)
private val DividerGray = Color(0xFFE0E0E0)

/**
 * Student Session Requests List Screen
 * Shows all session requests for a student
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRequestsScreen(
    navController: NavController,
    studentId: String,
    viewModel: SessionViewModel = viewModel<SessionViewModel>()
) {
    val requests by viewModel.studentRequests.observeAsState(emptyList())
    val serverError by viewModel.error.observeAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(studentId) {
        Log.d("StudentRequestsScreen", "Loading student requests for: $studentId")
        viewModel.loadStudentRequests(studentId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Session Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundGray
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
                if (!serverError.isNullOrEmpty()) {
                    StudentMessageCard(
                        message = serverError ?: "",
                        type = StudentMessageType.ERROR,
                        onDismiss = { }
                    )
                }

                errorMessage?.let { msg ->
                    StudentMessageCard(
                        message = msg,
                        type = StudentMessageType.ERROR,
                        onDismiss = { errorMessage = null }
                    )
                }

                successMessage?.let { msg ->
                    StudentMessageCard(
                        message = msg,
                        type = StudentMessageType.SUCCESS,
                        onDismiss = { successMessage = null }
                    )
                }
            }

            // Content area
            if (requests.isEmpty()) {
                StudentEmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = requests, key = { it.id }) { request ->
                        StudentRequestCard(
                            request = request,
                            sdf = sdf,
                            onClick = {
                                navController.navigate("student_request_details/${request.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

private enum class StudentMessageType {
    SUCCESS, ERROR
}

@Composable
private fun StudentMessageCard(
    message: String,
    type: StudentMessageType,
    onDismiss: () -> Unit
) {
    val (backgroundColor, iconColor, icon) = when (type) {
        StudentMessageType.SUCCESS -> Triple(
            Color(0xFFE8F5E9),
            SuccessGreen,
            Icons.Default.CheckCircle
        )
        StudentMessageType.ERROR -> Triple(
            Color(0xFFFFEBEE),
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
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = iconColor, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StudentEmptyStateView() {
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
                color = Color(0xFFE3F2FD),
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
                "You haven't requested any sessions yet.\nFind a tutor and request a session to get started!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StudentRequestCard(
    request: SessionRequest,
    sdf: SimpleDateFormat,
    onClick: () -> Unit
) {
    var tutorName by remember { mutableStateOf<String?>(null) }
    var isLoadingName by remember { mutableStateOf(true) }

    LaunchedEffect(request.teacherId) {
        isLoadingName = true
        try {
            Log.d("StudentRequestCard", "Fetching tutor name for ID: ${request.teacherId}")
            val tutorDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(request.teacherId)
                .get()
                .await()

            if (!tutorDoc.exists()) {
                Log.w("StudentRequestCard", "Tutor document does not exist for ID: ${request.teacherId}")
                tutorName = "Unknown Tutor"
            } else {
                val name = tutorDoc.getString("name")
                Log.d("StudentRequestCard", "Fetched name: $name for tutor ID: ${request.teacherId}")

                tutorName = if (!name.isNullOrBlank()) {
                    name
                } else {
                    Log.w("StudentRequestCard", "Name field is null or blank for tutor ID: ${request.teacherId}")
                    "Unknown Tutor"
                }
            }
        } catch (e: Exception) {
            Log.e("StudentRequestCard", "Error fetching tutor name for ${request.teacherId}: ${e.message}", e)
            tutorName = "Unknown Tutor"
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = statusInfo.backgroundColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        color = statusInfo.iconColor,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            statusInfo.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            statusInfo.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusInfo.iconColor
                        )
                        Text(
                            statusInfo.description,
                            style = MaterialTheme.typography.bodySmall,
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

            // Card content
            Column(modifier = Modifier.padding(16.dp)) {
                // Tutor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tutor: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(tutorName ?: "Loading...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subject: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(request.domain, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Time: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        (request.rescheduleProposedTime ?: request.requestedTime)?.let { sdf.format(it) } ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duration: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("${request.durationMinutes} minutes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                // Join Button (visible when session time is reached)
                if (canJoin || earlyJoinAllowed) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Note: This requires navController to be passed or use a callback
                            // For now, we'll need to modify the signature or handle navigation differently
                            onClick()
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

private data class StudentStatusInfo(
    val backgroundColor: Color,
    val iconColor: Color,
    val icon: ImageVector,
    val title: String,
    val description: String
)

private fun getStatusInfo(request: SessionRequest): StudentStatusInfo {
    return when (request.status) {
        "pending", "requested" -> StudentStatusInfo(
            backgroundColor = Color(0xFFE3F2FD),
            iconColor = PrimaryBlue,
            icon = Icons.Default.HourglassEmpty,
            title = "Pending",
            description = "Waiting for tutor response"
        )
        "reschedule_pending" -> StudentStatusInfo(
            backgroundColor = Color(0xFFFFF3E0),
            iconColor = AccentOrange,
            icon = Icons.Default.Schedule,
            title = "Reschedule Proposed",
            description = if (request.rescheduleProposedBy == "teacher") "Tutor proposed new time" else "You proposed new time"
        )
        "confirmed" -> StudentStatusInfo(
            backgroundColor = Color(0xFFE8F5E9),
            iconColor = SuccessGreen,
            icon = Icons.Default.CheckCircle,
            title = "Confirmed",
            description = "Session is scheduled"
        )
        "in_progress" -> StudentStatusInfo(
            backgroundColor = Color(0xFFE8F5E9),
            iconColor = Color(0xFF4CAF50),
            icon = Icons.Default.Videocam,
            title = "In Progress",
            description = "Session is currently active"
        )
        "completed" -> StudentStatusInfo(
            backgroundColor = Color(0xFFE3F2FD),
            iconColor = PrimaryBlue,
            icon = Icons.Default.Done,
            title = "Completed",
            description = "Session has ended"
        )
        "rejected" -> StudentStatusInfo(
            backgroundColor = Color(0xFFFFEBEE),
            iconColor = ErrorRed,
            icon = Icons.Default.Cancel,
            title = "Rejected",
            description = if (request.rejectedBy == "student") "You cancelled" else "Tutor declined"
        )
        else -> StudentStatusInfo(
            backgroundColor = Color(0xFFF5F5F5),
            iconColor = Color.Gray,
            icon = Icons.Default.Info,
            title = request.status.uppercase(),
            description = "Status unknown"
        )
    }
}

/**
 * Student Session Request Details Screen
 * Shows detailed view of a single session request
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSessionRequestDetailsScreen(
    navController: NavController,
    requestId: String,
    studentId: String,
    viewModel: SessionViewModel = viewModel<SessionViewModel>()
) {
    val requests by viewModel.studentRequests.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)

    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val TAG = "StudentSessionRequestDetailsScreen"

    // Find the specific request
    val request = requests.find { it.id == requestId }

    // Fetch user names
    var tutorName by remember { mutableStateOf<String?>(null) }
    var studentName by remember { mutableStateOf<String?>(null) }

    // Load requests if not already loaded
    LaunchedEffect(studentId) {
        if (requests.isEmpty()) {
            viewModel.loadStudentRequests(studentId)
        }
    }

    LaunchedEffect(request) {
        Log.d(TAG, "Loading request details for: $requestId")
        request?.let {
            try {
                Log.d(TAG, "Fetching tutor name for: ${it.teacherId}")
                val tutorDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(it.teacherId)
                    .get()
                    .await()
                val name = tutorDoc.getString("name")
                tutorName = if (!name.isNullOrBlank()) {
                    name
                } else {
                    Log.w(TAG, "Name field is null or blank for tutor ID: ${it.teacherId}")
                    "Unknown Tutor"
                }
                Log.d(TAG, "Tutor name: $tutorName")

                Log.d(TAG, "Fetching student name for: ${it.studentId}")
                val studentDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(it.studentId)
                    .get()
                    .await()
                studentName = studentDoc.getString("name") ?: "Unknown Student"
                Log.d(TAG, "Student name: $studentName")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching names: ${e.message}", e)
                tutorName = "Unknown Tutor"
                studentName = "Unknown Student"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Request Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (loading && request == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (request == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = "Not found",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Session request not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF5F5F5))
            ) {
                // Status Header
                DetailStatusHeader(request = request)

                Spacer(modifier = Modifier.height(16.dp))

                // Main Details Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Session Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tutor Info
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "Tutor",
                            value = tutorName ?: "Loading...",
                            color = Color(0xFF6200EE)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Domain
                        DetailRow(
                            icon = Icons.Default.School,
                            label = "Subject/Domain",
                            value = request.domain,
                            color = Color(0xFF1976D2)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Duration
                        DetailRow(
                            icon = Icons.Default.Timer,
                            label = "Duration",
                            value = "${request.durationMinutes} minutes",
                            color = Color(0xFFF57C00)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Scheduled Time
                        val scheduledTime = request.rescheduleProposedTime ?: request.requestedTime
                        DetailRow(
                            icon = Icons.AutoMirrored.Filled.EventNote,
                            label = "Scheduled Time",
                            value = scheduledTime?.let { sdf.format(it) } ?: "-",
                            color = Color(0xFF388E3C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reschedule Proposal Section (if applicable)
                if (request.status == "reschedule_pending" && request.rescheduleProposedTime != null) {
                    RescheduleProposalCard(
                        request = request,
                        proposedTime = request.rescheduleProposedTime,
                        sdf = sdf
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Message Section (if exists)
                if (request.message.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                request.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Timeline Section
                TimelineSection(request = request, sdf = sdf)

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                ActionButtonsSection(
                    request = request,
                    viewModel = viewModel,
                    onSuccess = {
                        Log.d(TAG, "Action success, refreshing requests")
                        viewModel.loadStudentRequests(studentId)
                    },
                    onJoinSession = { sessionId ->
                        navController.navigate("video_session/$sessionId")
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailStatusHeader(request: SessionRequest) {
    val statusInfo = getStatusInfo(request)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusInfo.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                color = statusInfo.iconColor,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = statusInfo.icon,
                    contentDescription = request.status,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    statusInfo.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusInfo.iconColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    statusInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
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
            modifier = Modifier.size(48.dp),
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxSize(),
                tint = color
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF757575),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121)
            )
        }
    }
}

@Composable
private fun RescheduleProposalCard(request: SessionRequest, proposedTime: Date, sdf: SimpleDateFormat) {
    val proposedByText = if (request.rescheduleProposedBy == "teacher") "By your tutor" else "By you"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFFF57C00),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Reschedule",
                        modifier = Modifier.padding(10.dp).fillMaxSize(),
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "New Time Proposed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Text(proposedByText, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBF360C))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Time", modifier = Modifier.size(24.dp), tint = Color(0xFFF57C00))
                    Text(sdf.format(proposedTime), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                }
            }
        }
    }
}

@Composable
private fun TimelineSection(request: SessionRequest, sdf: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            TimelineItem(title = "Request Created", time = request.createdAt?.let { sdf.format(it) } ?: "-", isCompleted = true)
            TimelineItem(title = "Last Updated", time = request.updatedAt?.let { sdf.format(it) } ?: "-", isCompleted = true)

            when (request.status) {
                "confirmed" -> TimelineItem(title = "Session Confirmed", time = "✓ Confirmed", isCompleted = true)
                "in_progress" -> TimelineItem(title = "Session In Progress", time = "🔴 Live", isCompleted = true)
                "completed" -> TimelineItem(title = "Session Completed", time = "✓ Done", isCompleted = true)
                "reschedule_pending" -> TimelineItem(title = "Reschedule Proposed", time = request.rescheduleProposedTime?.let { sdf.format(it) } ?: "-", isCompleted = false)
                "rejected" -> TimelineItem(title = "Request Rejected", time = "✗ Rejected", isCompleted = false)
                else -> TimelineItem(title = "Awaiting Tutor Response", time = "...", isCompleted = false)
            }
        }
    }
}

@Composable
private fun TimelineItem(title: String, time: String, isCompleted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = if (isCompleted) Color(0xFF388E3C) else Color(0xFFE0E0E0),
            shape = RoundedCornerShape(50)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = title,
                modifier = Modifier.padding(4.dp).fillMaxSize(),
                tint = if (isCompleted) Color.White else Color.Gray
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun ActionButtonsSection(
    request: SessionRequest,
    viewModel: SessionViewModel,
    onSuccess: () -> Unit,
    onJoinSession: (String) -> Unit
) {
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val TAG = "ActionButtonsSection"

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!errorMessage.isNullOrEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        when (request.status) {
            "reschedule_pending" -> {
                if (request.rescheduleProposedBy == "teacher") {
                    Button(
                        onClick = {
                            viewModel.acceptRequest(request) { res ->
                                if (res.isSuccess) onSuccess() else errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accept Proposed Time")
                    }

                    Button(
                        onClick = { showRescheduleDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Propose Different Time")
                    }
                } else {
                    Button(
                        onClick = { showRescheduleDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Propose Different Time")
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFF1976D2))
                            Text("Waiting for tutor to review your proposal", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0D47A1), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.rejectRequestBy(request.id, "student") { res ->
                            if (res.isSuccess) onSuccess() else errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Session Request")
                }
            }
            "pending", "requested" -> {
                OutlinedButton(
                    onClick = {
                        viewModel.rejectRequestBy(request.id, "student") { res ->
                            if (res.isSuccess) onSuccess() else errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Request")
                }
            }
            "confirmed", "in_progress" -> {
                SessionJoinButton(
                    scheduledTime = request.rescheduleProposedTime ?: request.requestedTime,
                    durationMinutes = request.durationMinutes,
                    onJoinClick = { onJoinSession(request.id) },
                    sessionStatus = request.status
                )
            }
            "rejected" -> {
                val rejectionText = when (request.rejectedBy) {
                    "student" -> "✗ You rejected this session"
                    "teacher" -> "✗ Tutor rejected this session"
                    else -> "✗ This session was rejected"
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(rejectionText, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showRescheduleDialog) {
        RescheduleDialog(
            request = request,
            onDismiss = { showRescheduleDialog = false },
            onConfirm = { selectedDate, selectedTime ->
                val calendar = Calendar.getInstance().apply {
                    time = selectedDate
                    set(Calendar.HOUR_OF_DAY, selectedTime.first)
                    set(Calendar.MINUTE, selectedTime.second)
                }
                viewModel.proposeReschedule(request.id, calendar.time, "student") { res ->
                    if (res.isSuccess) { showRescheduleDialog = false; onSuccess() }
                    else errorMessage = res.exceptionOrNull()?.message ?: "Failed"
                }
            }
        )
    }
}

@Composable
private fun RescheduleDialog(
    request: SessionRequest,
    onDismiss: () -> Unit,
    onConfirm: (Date, Pair<Int, Int>) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Propose Counter Time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select a different time for this session:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                OutlinedTextField(
                    value = if (selectedDate != null && selectedTime != null) {
                        val cal = Calendar.getInstance().apply {
                            time = selectedDate!!
                            set(Calendar.HOUR_OF_DAY, selectedTime!!.first)
                            set(Calendar.MINUTE, selectedTime!!.second)
                        }
                        sdf.format(cal.time)
                    } else "No time selected",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Selected Time") },
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Schedule") }
                )

                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            calendar.set(year, month, dayOfMonth)
                            selectedDate = calendar.time
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Date")
                }

                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(context, { _, hourOfDay, minute ->
                            val roundedMinute = (minute / 15) * 15
                            selectedTime = Pair(hourOfDay, roundedMinute)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedDate != null
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Time")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedDate != null && selectedTime != null) onConfirm(selectedDate!!, selectedTime!!) },
                enabled = selectedDate != null && selectedTime != null
            ) { Text("Send Proposal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
