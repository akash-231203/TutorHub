package com.example.tutorHub.ui.theme.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tutorHub.ViewModel.SessionViewModel
import com.example.tutorHub.data.classes.SessionRequest
import com.example.tutorHub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScheduleScreen(
    navController: NavController,
    viewModel: SessionViewModel = viewModel(),
    studentId: String,
    tutorId: String,
    domain: String,
    tutorName: String = ""
) {
    var duration by remember { mutableStateOf(60) }
    var message by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val loading by viewModel.loading.observeAsState(false)
    val serverError by viewModel.error.observeAsState()

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val displayFormat = SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    var showDatePicker by remember { mutableStateOf(false) }
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

    val initDateCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initDateCal.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayStartMillis && utcTimeMillis <= weekAheadMillis
            }
            override fun isSelectableYear(year: Int): Boolean {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                return year == currentYear || year == currentYear + 1
            }
        }
    )

    val nowCal = remember { Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = nowCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = nowCal.get(Calendar.MINUTE),
        is24Hour = true
    )

    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }
    var selectedCal by remember { mutableStateOf<Calendar?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    fun normalizeToQuarterHour(date: Date): Date {
        val mins = date.time / 60_000
        val rounded = (mins / 15.0).roundToLong() * 15
        return Date(rounded * 60_000)
    }

    fun updateSelectedFromPicker() {
        val millis = datePickerState.selectedDateMillis
        val h = selectedHour ?: timePickerState.hour
        val m = selectedMinute ?: timePickerState.minute
        if (millis != null) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedCal = cal
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Request Session",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tutor Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                color = SurfaceBlue,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(10.dp),
                                    tint = PrimaryBlue
                                )
                            }
                            Column {
                                Text(
                                    "Tutor",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (tutorName.isNotBlank()) tutorName else tutorId,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = DividerGray)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                color = SurfaceOrange,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.padding(10.dp),
                                    tint = AccentOrange
                                )
                            }
                            Column {
                                Text(
                                    "Subject",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    domain,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Date & Time Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Select Date & Time",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCal?.let { displayFormat.format(it.time) } ?: "",
                                onValueChange = { },
                                readOnly = true,
                                enabled = false,
                                label = { Text("Date & Time") },
                                placeholder = { Text("Tap to select") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, null, tint = PrimaryBlue)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderGray,
                                    disabledBorderColor = BorderGray,
                                    disabledTextColor = TextPrimary,
                                    disabledLabelColor = TextSecondary,
                                    disabledLeadingIconColor = PrimaryBlue,
                                    disabledPlaceholderColor = TextSecondary,
                                    disabledContainerColor = Color.White
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            color = SurfaceGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SuccessGreen
                                )
                                Text(
                                    "You can schedule up to 7 days in advance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }

                // Duration Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Session Duration",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (duration > 15) duration -= 15 },
                                enabled = !loading && duration > 15
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    color = if (duration > 15) SurfaceBlue else BackgroundLight,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        modifier = Modifier.padding(8.dp),
                                        tint = if (duration > 15) PrimaryBlue else BorderGray
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "$duration",
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = PrimaryBlue
                                    )
                                    Text(
                                        "min",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    "15 minute intervals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            IconButton(
                                onClick = { if (duration < 480) duration += 15 },
                                enabled = !loading && duration < 480
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    color = if (duration < 480) SurfaceBlue else BackgroundLight,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase",
                                        modifier = Modifier.padding(8.dp),
                                        tint = if (duration < 480) PrimaryBlue else BorderGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Message Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Additional Message (Optional)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text("Any specific topics or questions?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderGray,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PrimaryBlue
                            )
                        )
                    }
                }

                // Error Messages
                if (!serverError.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed)
                            Text(serverError ?: "", color = ErrorRed)
                        }
                    }
                }

                if (localError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed)
                            Text(localError ?: "", color = ErrorRed)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Submit Button
                val selectedDate = selectedCal?.time
                val inRange = selectedCal?.let { cal ->
                    val d = cal.apply { set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    d >= todayStartMillis && d <= weekAheadMillis
                } ?: false
                val canSend = selectedDate != null && selectedDate.after(Date()) && inRange && !loading

                Button(
                    onClick = {
                        localError = null
                        val sel = selectedCal ?: run {
                            localError = "Please choose date & time"
                            return@Button
                        }
                        showConfirmDialog = true
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Send Request",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selMillis = datePickerState.selectedDateMillis
                        if (selMillis == null) {
                            localError = "Please select a date"
                            return@TextButton
                        }
                        if (selMillis < todayStartMillis || selMillis > weekAheadMillis) {
                            localError = "Please pick a date within the next 7 days"
                            return@TextButton
                        }
                        val calNow = Calendar.getInstance()
                        timePickerState.hour = selectedHour ?: calNow.get(Calendar.HOUR_OF_DAY)
                        timePickerState.minute = selectedMinute ?: calNow.get(Calendar.MINUTE)
                        showDatePicker = false
                        showTimePicker = true
                    }) { Text("Next") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time Picker Dialog
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        updateSelectedFromPicker()
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                },
                title = { Text("Select Time", fontWeight = FontWeight.Bold) },
                text = { TimePicker(state = timePickerState) },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Confirmation Dialog
        if (showConfirmDialog) {
            val normalizedText = selectedCal?.let { cal ->
                val normalized = normalizeToQuarterHour(cal.time)
                displayFormat.format(normalized)
            } ?: ""
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                        Text("Confirm Request", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Your session will be scheduled for:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                normalizedText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SuccessGreen,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text(
                            "Duration: $duration minutes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            color = SurfaceOrange,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = AccentOrange
                                )
                                Text(
                                    "Time normalized to nearest 15 minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentOrange
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            val sel = selectedCal ?: return@Button
                            val normalized = normalizeToQuarterHour(sel.time)
                            val req = SessionRequest(
                                studentId = studentId,
                                teacherId = tutorId,
                                domain = domain,
                                requestedTime = normalized,
                                durationMinutes = duration,
                                message = message,
                                createdAt = Date(),
                                updatedAt = Date()
                            )
                            viewModel.createRequest(req) { res: Result<String> ->
                                if (res.isSuccess) {
                                    navController.popBackStack()
                                } else {
                                    localError = res.exceptionOrNull()?.message ?: "Failed to send request"
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Send Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) { Text("Review") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
