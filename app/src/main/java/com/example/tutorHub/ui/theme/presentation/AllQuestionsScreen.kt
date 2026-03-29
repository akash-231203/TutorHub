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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tutorHub.ViewModel.QuestionViewModel
import com.example.tutorHub.data.classes.QuestionPost

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AllQuestionsScreen(
    navController: NavController,
    tutorId: String,
    type: String,
    viewModel: QuestionViewModel
) {
    LaunchedEffect(tutorId, type) { viewModel.loadAllForTutor(tutorId, type) }

    val list by viewModel.questions.observeAsState(emptyList())
    val isPosting by viewModel.isPosting.observeAsState(false)
    val solutionStatus by viewModel.solutionStatus.observeAsState()
    var description by remember { mutableStateOf("") }
    var selectedQ by remember { mutableStateOf<QuestionPost?>(null) }
    var attachment by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachment = uri }

    solutionStatus?.let { res ->
        val success = res.isSuccess
        AlertDialog(
            onDismissRequest = { viewModel.clearSolutionStatus() },
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
                        viewModel.clearSolutionStatus()
                        selectedQ = null
                        description = ""
                        attachment = null
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
                        if (type == "written") "Free Questions" else "Paid Requests",
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            if (list.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No Questions Available",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check back later for new questions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { q ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            q.title.ifBlank { "Question" },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (q.description.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                q.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 4
                                            )
                                        }
                                    }
                                    if (type == "video") {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("PAID", style = MaterialTheme.typography.labelSmall) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }
                                }

                                q.imageUrl?.let { url ->
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedImageUrl = url }
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Question image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.ZoomIn,
                                                    contentDescription = "View",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("View", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        selectedQ = q
                                        attachment = null
                                        description = ""
                                        val mime = if (type == "written") "image/*" else "video/*"
                                        launcher.launch(mime)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Attach Solution")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attach Solution Dialog
    if (selectedQ != null && attachment != null) {
        AlertDialog(
            onDismissRequest = { selectedQ = null; attachment = null },
            title = {
                Text(
                    "Attach ${if (type == "written") "Image" else "Video"} Solution",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Describe your solution...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = selectedQ ?: return@Button
                        val uri = attachment ?: return@Button
                        viewModel.postSolutionForQuestion(
                            questionId = q.id,
                            tutorId = tutorId,
                            description = description,
                            attachment = uri,
                            isVideo = type != "written"
                        )
                    },
                    enabled = !isPosting && description.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPosting) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    ) else Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedQ = null; attachment = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Full-screen Image Preview
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
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
