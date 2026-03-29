package com.example.tutorHub.ui.theme.presentation

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tutorHub.ViewModel.QuestionViewModel
import com.example.tutorHub.ViewModel.StudentDashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    navController: NavController,
    viewModel: StudentDashboardViewModel,
    studentId: String,
    questionVM: QuestionViewModel = viewModel()
) {
    val dashboard = viewModel.dashboardData.observeAsState()

    LaunchedEffect(studentId) {
        viewModel.fetchDashboard(studentId)
        questionVM.loadAllQuestionsForStudent(studentId)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, studentId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchDashboard(studentId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var postMenuExpanded by remember { mutableStateOf(false) }
    val allQuestions by questionVM.studentAll.observeAsState(emptyList())
    val solutions by questionVM.solutions.observeAsState(emptyList())
    val isLoadingSolutions by questionVM.isLoadingSolutions.observeAsState(false)
    var showSolutionsFor by remember { mutableStateOf<String?>(null) }
    var isCheckingSolutions by rememberSaveable { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Student Dashboard",
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
                            "Your Learning Journey",
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
                            StudentStatCard(
                                title = "Questions",
                                value = "${dashboard.value?.postedQuestions ?: 0}",
                                icon = Icons.Default.QuestionAnswer,
                                color = Color(0xFFE3F2FD),
                                iconColor = Color(0xFF1976D2),
                                modifier = Modifier.weight(1f)
                            )
                            StudentStatCard(
                                title = "Sessions",
                                value = "${dashboard.value?.sessionsAttended ?: 0}",
                                icon = Icons.Default.EventAvailable,
                                color = Color(0xFFE8F5E9),
                                iconColor = Color(0xFF388E3C),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StudentStatCard(
                                title = "Pending",
                                value = "${dashboard.value?.queriesPending ?: 0}",
                                icon = Icons.Default.HourglassEmpty,
                                color = Color(0xFFFFF3E0),
                                iconColor = Color(0xFFF57C00),
                                modifier = Modifier.weight(1f)
                            )
                            StudentStatCard(
                                title = "Solved",
                                value = "${dashboard.value?.solvedQuestions ?: 0}",
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFFFCE4EC),
                                iconColor = Color(0xFFC2185B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Quick Actions Section
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF212121)
                )
            }

            // Request Video Session Button
            item {
                ElevatedCard(
                    onClick = { navController.navigate("select_domain/schedule") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.VideoCall,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp),
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Request Video Session",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                "Get personalized help from tutors",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            // My Session Requests Button
            item {
                ElevatedCard(
                    onClick = { navController.navigate("student_requests/$studentId") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6200EE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp),
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "My Session Requests",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                "View and manage your requests",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            // Post Question Button
            item {
                ElevatedCard(
                    onClick = { postMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp),
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Post a Question",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                "Get answers from expert tutors",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                DropdownMenu(
                    expanded = postMenuExpanded,
                    onDismissRequest = { postMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Written Solution") },
                        onClick = {
                            postMenuExpanded = false
                            navController.navigate("post_written")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Video Explanation") },
                        onClick = {
                            postMenuExpanded = false
                            navController.navigate("select_domain/post")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.VideoCall, contentDescription = null)
                        }
                    )
                }
            }

            // Section Header for Questions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Questions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF212121)
                    )
                }
            }

            if (allQuestions.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "You haven't posted any questions yet",
                        icon = Icons.Default.QuestionAnswer
                    )
                }
            } else {
                items(allQuestions, key = { it.id }) { q ->
                    QuestionCard(
                        question = q,
                        onViewSolutions = {
                            if (isCheckingSolutions) return@QuestionCard
                            val id = q.id
                            if (id.isBlank()) {
                                showSolutionsFor = null
                                return@QuestionCard
                            }
                            isCheckingSolutions = true
                            questionVM.fetchSolutionsForDecision(id) { count ->
                                if (count > 0) {
                                    navController.navigate("student_solution_screen/$id")
                                } else {
                                    showSolutionsFor = id
                                    questionVM.loadSolutionsForQuestion(id)
                                }
                                isCheckingSolutions = false
                            }
                        }
                    )
                }
            }
        }
    }

    // Solutions Dialog
    if (showSolutionsFor != null) {
        AlertDialog(
            onDismissRequest = { showSolutionsFor = null; questionVM.clearSolutions() },
            title = {
                Text(
                    "Solutions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(Modifier.heightIn(max = 400.dp)) {
                    if (isLoadingSolutions) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (solutions.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF9E9E9E)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No solutions available yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF757575),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(solutions) { s ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            s.description.ifBlank {
                                                if (s.attachmentType == "video") "Video solution" else "Image solution"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        if (s.attachmentType == "image") {
                                            Spacer(Modifier.height(8.dp))
                                            AsyncImage(
                                                model = s.attachmentUrl,
                                                contentDescription = "Solution image",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else if (s.attachmentType == "video") {
                                            Spacer(Modifier.height(8.dp))
                                            Surface(
                                                color = Color(0xFFE3F2FD),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.VideoLibrary,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = Color(0xFF1976D2)
                                                    )
                                                    Text(
                                                        "Video solution attached",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF1976D2),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSolutionsFor = null; questionVM.clearSolutions() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Logout Confirmation Dialog
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
                        navController.navigate("login") {
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
private fun StudentStatCard(
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
    onViewSolutions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                question.title.ifBlank { "Question" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF212121)
            )

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
                AsyncImage(
                    model = url,
                    contentDescription = "Question image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onViewSolutions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Solutions", fontWeight = FontWeight.SemiBold)
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
