package com.example.tutorHub.ui.theme.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tutorHub.ViewModel.SessionViewModel
import com.example.tutorHub.data.classes.SessionRequest
import com.example.tutorHub.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "TeacherSessionDetailsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSessionRequestDetailsScreen(
    navController: NavController,
    requestId: String,
    teacherId: String,
    viewModel: SessionViewModel = viewModel<SessionViewModel>()
) {
    val requests by viewModel.teacherRequests.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)

    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val displayFormat = SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    // Find the specific request
    val request = requests.find { it.id == requestId }

    // Fetch user names
    var studentName by remember { mutableStateOf<String?>(null) }
    var tutorName by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showRejectDialog by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showAcceptDialog by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    // Load requests if not already loaded
    LaunchedEffect(teacherId) {
        if (requests.isEmpty()) {
            viewModel.loadTeacherRequests(teacherId)
        }
    }

    LaunchedEffect(request) {
        request?.let {
            try {
                Log.d(TAG, "Fetching student name for: ${it.studentId}")
                val studentDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(it.studentId)
                    .get()
                    .await()
                val name = studentDoc.getString("name")
                studentName = if (!name.isNullOrBlank()) {
                    name
                } else {
                    Log.w(TAG, "Name field is null or blank for student ID: ${it.studentId}")
                    "Unknown Student"
                }
                Log.d(TAG, "Student name: $studentName")

                Log.d(TAG, "Fetching tutor name for: ${it.teacherId}")
                val tutorDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(it.teacherId)
                    .get()
                    .await()
                tutorName = tutorDoc.getString("name") ?: "Unknown Tutor"
                Log.d(TAG, "Tutor name: $tutorName")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching names: ${e.message}", e)
                studentName = "Unknown Student"
                tutorName = "Unknown Tutor"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
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
                        Icons.Default.EventBusy,
                        contentDescription = "Not found",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Session not found", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Status Header
                TeacherStatusHeader(request = request)

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Section (for pending/reschedule_pending requests)
                if (request.status == "pending" || request.status == "requested" ||
                    (request.status == "reschedule_pending" && request.rescheduleProposedBy == "student")) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "RESPOND TO REQUEST",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Accept Button
                            Button(
                                onClick = { showAcceptDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen,
                                    contentColor = Color.White
                                ),
                                enabled = !loading
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Accept Session",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Reschedule Button
                            OutlinedButton(
                                onClick = { showRescheduleDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AccentOrange
                                ),
                                border = androidx.compose.foundation.BorderStroke(2.dp, AccentOrange),
                                enabled = !loading
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Propose Reschedule",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Reject Button
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ErrorRed
                                ),
                                border = androidx.compose.foundation.BorderStroke(2.dp, ErrorRed),
                                enabled = !loading
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Reject Request",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Error message
                            if (actionError != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = SurfaceRed,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = ErrorRed
                                        )
                                        Text(
                                            actionError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ErrorRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Session Details Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "SESSION DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Student Info
                        TeacherDetailRow(
                            icon = Icons.Default.Person,
                            label = "Student",
                            value = studentName ?: "Loading...",
                            color = PrimaryBlue
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DividerGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Domain
                        TeacherDetailRow(
                            icon = Icons.Default.School,
                            label = "Subject",
                            value = request.domain,
                            color = Color(0xFF9C27B0)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DividerGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Duration
                        TeacherDetailRow(
                            icon = Icons.Default.Timer,
                            label = "Duration",
                            value = "${request.durationMinutes} minutes",
                            color = AccentOrange
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DividerGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scheduled Time
                        val scheduledTime = request.rescheduleProposedTime ?: request.requestedTime
                        TeacherDetailRow(
                            icon = Icons.Default.Event,
                            label = "Scheduled Time",
                            value = scheduledTime?.let { sdf.format(it) } ?: "-",
                            color = SuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Message Section (if exists)
                if (request.message.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "MESSAGE FROM STUDENT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = BackgroundLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    request.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Join Session Section (for confirmed and in_progress sessions)
                if (request.status == "confirmed" || request.status == "in_progress") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "JOIN SESSION",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Session Join Button with countdown timer
                            SessionJoinButton(
                                scheduledTime = request.rescheduleProposedTime ?: request.requestedTime,
                                durationMinutes = request.durationMinutes,
                                onJoinClick = {
                                    navController.navigate("video_session/${request.id}")
                                },
                                sessionStatus = request.status
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Timeline Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "TIMELINE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TeacherTimelineItem(
                            title = "Request Created",
                            time = request.createdAt?.let { sdf.format(it) } ?: "-",
                            isCompleted = true
                        )

                        TeacherTimelineItem(
                            title = "Last Updated",
                            time = request.updatedAt?.let { sdf.format(it) } ?: "-",
                            isCompleted = true
                        )

                        when (request.status) {
                            "confirmed" -> {
                                TeacherTimelineItem(
                                    title = "Session Confirmed",
                                    time = "✓ Confirmed",
                                    isCompleted = true
                                )
                            }
                            "in_progress" -> {
                                TeacherTimelineItem(
                                    title = "Session In Progress",
                                    time = "🔴 Live",
                                    isCompleted = true
                                )
                            }
                            "completed" -> {
                                TeacherTimelineItem(
                                    title = "Session Completed",
                                    time = "✓ Done",
                                    isCompleted = true
                                )
                            }
                            "reschedule_pending" -> {
                                TeacherTimelineItem(
                                    title = "Reschedule Proposed",
                                    time = request.rescheduleProposedTime?.let { sdf.format(it) } ?: "-",
                                    isCompleted = false
                                )
                            }
                            "rejected" -> {
                                TeacherTimelineItem(
                                    title = "Session Rejected",
                                    time = "✗ Rejected",
                                    isCompleted = false
                                )
                            }
                            else -> {
                                TeacherTimelineItem(
                                    title = "Awaiting Review",
                                    time = "Pending...",
                                    isCompleted = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Accept Confirmation Dialog
        if (showAcceptDialog) {
            AlertDialog(
                onDismissRequest = { showAcceptDialog = false },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Accept Session Request",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Confirm that you want to accept this session request:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = SurfaceGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Time: ${(request?.rescheduleProposedTime ?: request?.requestedTime)?.let { displayFormat.format(it) } ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = SuccessGreen
                                )
                                Text(
                                    "Duration: ${request?.durationMinutes} minutes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            actionError = null
                            request?.let { req ->
                                viewModel.acceptRequest(req) { result ->
                                    if (result.isSuccess) {
                                        showAcceptDialog = false
                                        viewModel.loadTeacherRequests(teacherId)
                                    } else {
                                        actionError = result.exceptionOrNull()?.message ?: "Failed to accept request"
                                        showAcceptDialog = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Accept Session")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAcceptDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Reject Confirmation Dialog
        if (showRejectDialog) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Reject Session Request",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to reject this session request? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            actionError = null
                            request?.id?.let { reqId ->
                                viewModel.rejectRequestBy(reqId, "teacher") { result ->
                                    if (result.isSuccess) {
                                        showRejectDialog = false
                                        viewModel.loadTeacherRequests(teacherId)
                                    } else {
                                        actionError = result.exceptionOrNull()?.message ?: "Failed to reject request"
                                        showRejectDialog = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Reject")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Reschedule Dialog
        if (showRescheduleDialog) {
            var selectedDate by remember { mutableStateOf<Calendar?>(null) }
            var showDatePicker by remember { mutableStateOf(true) }
            var showTimePicker by remember { mutableStateOf(false) }

            val todayStartMillis = remember {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            val weekAheadMillis = remember { todayStartMillis + 7L * 24L * 60L * 60L * 1000L }

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = todayStartMillis,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= todayStartMillis && utcTimeMillis <= weekAheadMillis
                    }
                    override fun isSelectableYear(year: Int): Boolean {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        return year == currentYear || year == currentYear + 1
                    }
                }
            )

            val timePickerState = rememberTimePickerState(
                initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
                is24Hour = true
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = {
                        showDatePicker = false
                        showRescheduleDialog = false
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                showDatePicker = false
                                showTimePicker = true
                            }
                        }) {
                            Text("Next")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            showRescheduleDialog = false
                        }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = {
                        showTimePicker = false
                        showRescheduleDialog = false
                    },
                    title = { Text("Select Time", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            TimePicker(state = timePickerState)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = millis
                                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        set(Calendar.MINUTE, timePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDate = cal

                                    actionError = null
                                    request?.id?.let { reqId ->
                                        viewModel.proposeReschedule(reqId, cal.time, "teacher") { result ->
                                            if (result.isSuccess) {
                                                showTimePicker = false
                                                showRescheduleDialog = false
                                                viewModel.loadTeacherRequests(teacherId)
                                            } else {
                                                actionError = result.exceptionOrNull()?.message ?: "Failed to propose reschedule"
                                                showTimePicker = false
                                                showRescheduleDialog = false
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            enabled = !loading
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Propose Time")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showTimePicker = false
                            showRescheduleDialog = false
                        }) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TeacherStatusHeader(request: SessionRequest) {
    data class StatusInfo(
        val color: Color,
        val iconBackground: Color,
        val icon: ImageVector,
        val text: String,
        val description: String
    )

    val statusInfo = when (request.status) {
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
            description = if (request.rescheduleProposedBy == "student")
                "Student proposed a new time"
            else
                "You proposed a new time - waiting for student"
        )
        "confirmed" -> StatusInfo(
            color = SurfaceGreen,
            iconBackground = SuccessGreen,
            icon = Icons.Default.CheckCircle,
            text = "Session Confirmed",
            description = "Session is scheduled and ready to join"
        )
        "in_progress" -> StatusInfo(
            color = SurfaceGreen,
            iconBackground = Color(0xFF4CAF50),
            icon = Icons.Default.Videocam,
            text = "Session In Progress",
            description = "The session is currently active"
        )
        "completed" -> StatusInfo(
            color = SurfaceBlue,
            iconBackground = PrimaryBlue,
            icon = Icons.Default.Done,
            text = "Session Completed",
            description = "This session has been completed"
        )
        "rejected" -> StatusInfo(
            color = SurfaceRed,
            iconBackground = ErrorRed,
            icon = Icons.Default.Cancel,
            text = "Session Rejected",
            description = if (request.rejectedBy == "teacher")
                "You rejected this request"
            else
                "Student cancelled this request"
        )
        else -> StatusInfo(
            color = BackgroundLight,
            iconBackground = Color.Gray,
            icon = Icons.Default.Info,
            text = request.status.uppercase(),
            description = "Status information unavailable"
        )
    }

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
        }
    }
}

@Composable
private fun TeacherDetailRow(
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

@Composable
private fun TeacherTimelineItem(title: String, time: String, isCompleted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            color = if (isCompleted) SuccessGreen else Color(0xFFBDBDBD),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Schedule,
                contentDescription = title,
                modifier = Modifier.padding(6.dp),
                tint = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                time,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
