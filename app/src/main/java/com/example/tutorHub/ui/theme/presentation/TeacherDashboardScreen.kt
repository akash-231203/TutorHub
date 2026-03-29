package com.example.tutorHub.ui.theme.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tutorHub.ViewModel.QuestionViewModel
import com.example.tutorHub.ViewModel.TeacherDashboardViewModel
import com.example.tutorHub.util.formatRating
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    viewModel: TeacherDashboardViewModel,
    teacherId: String,
    navController: NavController? = null,
    questionVM: QuestionViewModel = viewModel()
) {
    val dashboard = viewModel.dashboardData.observeAsState()

    LaunchedEffect(teacherId) {
        viewModel.fetchDashboard(teacherId)
        questionVM.loadTutorFeeds(teacherId)
    }

    val unpaid by questionVM.unpaid.observeAsState(emptyList())
    val paid by questionVM.paid.observeAsState(emptyList())
    val isPosting by questionVM.isPosting.observeAsState(false)
    val solutionStatus by questionVM.solutionStatus.observeAsState()

    var attachForQuestionId by remember { mutableStateOf<String?>(null) }
    var attachDescription by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var expectVideo by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedUri = uri
    }

    solutionStatus?.let { res ->
        val success = res.isSuccess
        AlertDialog(
            onDismissRequest = { questionVM.clearSolutionStatus() },
            title = {
                Text(
                    if (success) "Solution Submitted" else "Error",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(res.exceptionOrNull()?.message ?: if (success) "Your solution has been submitted successfully." else "Unknown error")
            },
            confirmButton = {
                Button(
                    onClick = {
                        questionVM.clearSolutionStatus()
                        attachForQuestionId = null
                        attachDescription = ""
                        pickedUri = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tutor Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { navController?.navigate("teacher_requests/$teacherId") }) {
                        Icon(Icons.Default.Mail, "Requests", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Stats Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Your Performance",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TutorStatCard(
                                title = "Sessions",
                                value = "${dashboard.value?.sessionsConducted ?: 0}",
                                icon = Icons.Default.EventAvailable,
                                color = Color(0xFFE3F2FD),
                                iconColor = Color(0xFF1976D2),
                                modifier = Modifier.weight(1f)
                            )
                            TutorStatCard(
                                title = "Rating",
                                value = formatRating(
                                    dashboard.value?.averageRating ?: 0.0,
                                    dashboard.value?.reviewsReceived ?: 0
                                ),
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFF3E0),
                                iconColor = Color(0xFFF57C00),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TutorStatCard(
                                title = "Earnings",
                                value = "₹${String.format(Locale.getDefault(), "%.0f", dashboard.value?.earnings ?: 0.0)}",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFFE8F5E9),
                                iconColor = Color(0xFF388E3C),
                                modifier = Modifier.weight(1f)
                            )
                            TutorStatCard(
                                title = "Reviews",
                                value = "${dashboard.value?.reviewsReceived ?: 0}",
                                icon = Icons.Default.RateReview,
                                color = Color(0xFFFCE4EC),
                                iconColor = Color(0xFFC2185B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Unpaid Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Help Students (Free)",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF212121)
                        )
                        Text(
                            "Answer questions to build your reputation",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                    TextButton(onClick = { navController?.navigate("all_questions/written") }) {
                        Text("View All", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (unpaid.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No questions available",
                        icon = Icons.Default.QuestionAnswer
                    )
                }
            } else {
                items(unpaid.take(3), key = { it.id }) { q ->
                    QuestionCard(
                        question = q,
                        onAttach = {
                            expectVideo = false
                            attachForQuestionId = q.id
                            attachDescription = ""
                            pickedUri = null
                            launcher.launch("image/*")
                        },
                        onImageClick = { url -> selectedImageUrl = url }
                    )
                }
            }

            // Paid Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Paid Video Requests",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF212121)
                        )
                        Text(
                            "Earn money by providing video explanations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                    TextButton(onClick = { navController?.navigate("all_questions/video") }) {
                        Text("View All", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (paid.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No video requests available",
                        icon = Icons.Default.VideoLibrary
                    )
                }
            } else {
                items(paid.take(3), key = { it.id }) { q ->
                    QuestionCard(
                        question = q,
                        onAttach = {
                            expectVideo = true
                            attachForQuestionId = q.id
                            attachDescription = ""
                            pickedUri = null
                            launcher.launch("video/*")
                        },
                        onImageClick = { url -> selectedImageUrl = url },
                        isPaid = true
                    )
                }
            }
        }
    }

    // Attach solution dialog
    if (attachForQuestionId != null && pickedUri != null) {
        AlertDialog(
            onDismissRequest = { attachForQuestionId = null; pickedUri = null },
            title = {
                Text(
                    "Attach ${if (expectVideo) "Video" else "Image"} Solution",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = attachDescription,
                        onValueChange = { attachDescription = it },
                        label = { Text("Description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qid = attachForQuestionId ?: return@Button
                        val uri = pickedUri ?: return@Button
                        questionVM.postSolutionForQuestion(
                            questionId = qid,
                            tutorId = teacherId,
                            description = attachDescription,
                            attachment = uri,
                            isVideo = expectVideo
                        )
                    },
                    enabled = !isPosting && attachDescription.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPosting) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    ) else Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { attachForQuestionId = null; pickedUri = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Full-screen image preview
    if (selectedImageUrl != null) {
        Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Question image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedImageUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.error)
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        FirebaseAuth.getInstance().signOut()
                        navController?.navigate("login") {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TutorStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = iconColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = iconColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF212121)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: com.example.tutorHub.data.classes.QuestionPost,
    onAttach: () -> Unit,
    onImageClick: (String) -> Unit,
    isPaid: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            question.title.ifBlank { "Question" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPaid) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF388E3C)
                                    )
                                    Text(
                                        "PAID",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF388E3C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (question.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    question.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575),
                    maxLines = 3
                )
            }

            question.imageUrl?.let { url ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(url) }
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Question image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAttach,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaid) Color(0xFF388E3C) else Color(0xFF1976D2)
                )
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Attach Solution", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(40.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
