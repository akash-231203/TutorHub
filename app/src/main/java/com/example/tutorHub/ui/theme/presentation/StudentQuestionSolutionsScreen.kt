package com.example.tutorHub.ui.theme.presentation

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tutorHub.ViewModel.QuestionViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.delay
import java.util.Locale

// Top-level helper so it can be referenced anywhere in this file
private fun enqueueDownload(
    dm: DownloadManager,
    downloadingIds: MutableMap<Long, String>,
    progressMap: MutableMap<String, Float>,
    indeterminateMap: MutableMap<String, Boolean>,
    solId: String,
    url: String,
    isVideo: Boolean
) {
    val uri = Uri.parse(url)
    val fileName = if (isVideo) "solution_${solId}.mp4" else "solution_${solId}.jpg"
    val mime = if (isVideo) "video/mp4" else "image/jpeg"
    val req = DownloadManager.Request(uri)
        .setTitle(fileName)
        .setMimeType(mime)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .setVisibleInDownloadsUi(true)
    @Suppress("DEPRECATION")
    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    val id = dm.enqueue(req)
    downloadingIds[id] = solId
    progressMap[solId] = 0f
    indeterminateMap[solId] = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuestionSolutionsScreen(
    navController: NavController,
    questionId: String,
    viewModel: QuestionViewModel
) {
    LaunchedEffect(questionId) { viewModel.loadSolutionsForQuestion(questionId) }

    val solutions by viewModel.solutions.observeAsState(emptyList())
    val isLoading by viewModel.isLoadingSolutions.observeAsState(false)
    val ratingStatus by viewModel.ratingStatus.observeAsState()
    val deleteStatus by viewModel.deleteStatus.observeAsState()

    val context = LocalContext.current
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val downloadedMap = remember { mutableStateMapOf<String, Uri>() }
    val downloadingIds = remember { mutableStateMapOf<Long, String>() }
    val progressMap = remember { mutableStateMapOf<String, Float>() }
    val indeterminateMap = remember { mutableStateMapOf<String, Boolean>() }

    val needsLegacyWrite = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q }
    var pendingDownload by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val req = pendingDownload
        pendingDownload = null
        if (granted && req != null) {
            enqueueDownload(
                dm = dm,
                downloadingIds = downloadingIds,
                progressMap = progressMap,
                indeterminateMap = indeterminateMap,
                solId = req.first,
                url = req.second,
                isVideo = req.third
            )
        }
    }

    // Receiver for download complete
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    val solId = downloadingIds.remove(id) ?: return
                    // Prefer robust URI from DownloadManager
                    val fileUri = dm.getUriForDownloadedFile(id)
                    if (fileUri != null) {
                        downloadedMap[solId] = fileUri
                        indeterminateMap.remove(solId)
                        progressMap[solId] = 1f
                        return
                    }
                    // Fallback to querying columns if needed
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor: Cursor = dm.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            localUri?.let { downloadedMap[solId] = Uri.parse(it) }
                            indeterminateMap.remove(solId)
                            progressMap[solId] = 1f
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            indeterminateMap.remove(solId)
                            progressMap.remove(solId)
                        }
                    }
                    cursor.close()
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        // Use ContextCompat to set proper flags on API 33+ while remaining backward-compatible
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Periodically query DownloadManager for progress of active downloads
    LaunchedEffect(Unit) {
        while (true) {
            if (downloadingIds.isNotEmpty()) {
                val ids = downloadingIds.keys.toList()
                ids.forEach { id ->
                    val solId = downloadingIds[id] ?: return@forEach
                    val query = DownloadManager.Query().setFilterById(id)
                    dm.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            val soFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            when (status) {
                                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                    if (total > 0) {
                                        indeterminateMap[solId] = false
                                        progressMap[solId] = (soFar.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        indeterminateMap[solId] = true
                                        progressMap[solId] = 0f
                                    }
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    indeterminateMap.remove(solId)
                                    progressMap[solId] = 1f
                                    // Will also be finalized by receiver
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    indeterminateMap.remove(solId)
                                    progressMap.remove(solId)
                                    downloadingIds.remove(id)
                                }
                            }
                        }
                    }
                }
            }
            delay(300)
        }
    }

    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Solutions",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mark as Solved")
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
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading solutions...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (solutions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "No Solutions Yet",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Tutors will provide solutions soon",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(solutions, key = { it.id }) { s ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    // Solution Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (s.attachmentType == "video") Icons.Default.VideoLibrary else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                s.description.ifBlank {
                                                    if (s.attachmentType == "video") "Video Solution" else "Image Solution"
                                                },
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // Media Content
                                    val localUri = downloadedMap[s.id]
                                    if (s.attachmentType == "image") {
                                        AsyncImage(
                                            model = localUri ?: Uri.parse(s.attachmentUrl),
                                            contentDescription = "Solution image",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp, max = 100.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else if (s.attachmentType == "video") {
                                        if (localUri != null) {
                                            AndroidView(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(250.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                factory = { ctx ->
                                                    VideoView(ctx).apply {
                                                        setVideoURI(localUri)
                                                        setOnPreparedListener { mp ->
                                                            mp.isLooping = false
                                                            start()
                                                        }
                                                    }
                                                },
                                                update = { vv -> vv.setVideoURI(localUri); vv.start() }
                                            )
                                        } else {
                                            Text(
                                                "Download to get the video solution.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.height(16.dp))

                                    // Download Section
                                    val isIndeterminate = indeterminateMap[s.id] == true
                                    val progress = progressMap[s.id]

                                    if (progress != null && progress in 0f..0.9999f) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isIndeterminate) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(32.dp),
                                                        strokeWidth = 3.dp
                                                    )
                                                } else {
                                                    CircularProgressIndicator(
                                                        progress = { progress },
                                                        modifier = Modifier.size(32.dp),
                                                        strokeWidth = 3.dp
                                                    )
                                                }
                                                Spacer(Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        "Downloading...",
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    )
                                                    val pct = if (isIndeterminate) "Preparing..." else "${(progress * 100).toInt()}%"
                                                    Text(
                                                        pct,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else if (progress == 1f) {
                                        Button(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Downloaded")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val wantsVideo = s.attachmentType == "video"
                                                if (needsLegacyWrite && ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    pendingDownload = Triple(s.id, s.attachmentUrl, wantsVideo)
                                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                } else {
                                                    enqueueDownload(
                                                        dm = dm,
                                                        downloadingIds = downloadingIds,
                                                        progressMap = progressMap,
                                                        indeterminateMap = indeterminateMap,
                                                        solId = s.id,
                                                        url = s.attachmentUrl,
                                                        isVideo = wantsVideo
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Download Solution")
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.height(16.dp))

                                    // Rating Section with continuous slider (0.0 to 5.0)
                                    val studentId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    val alreadyRated = s.ratedBy == studentId && s.rating != null
                                    var rating by remember(s.id, s.ratedBy, s.rating) { mutableFloatStateOf((s.rating ?: 5.0).toFloat()) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    if (alreadyRated) "You rated this solution" else "Rate this solution",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        String.format(Locale.getDefault(), "%.1f", rating),
                                                        style = MaterialTheme.typography.headlineSmall.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "/5.0",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(12.dp))

                                            // Continuous slider from 0.0 to 5.0
                                            Slider(
                                                value = rating,
                                                onValueChange = { rating = it },
                                                valueRange = 0f..5f,
                                                enabled = !alreadyRated,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = MaterialTheme.colorScheme.primary,
                                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            // Star rating display
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                repeat(5) { index ->
                                                    Icon(
                                                        if (index < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(28.dp),
                                                        tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                    )
                                                    if (index < 4) Spacer(Modifier.width(4.dp))
                                                }
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            Button(
                                                onClick = {
                                                    if (!alreadyRated && studentId.isNotBlank()) {
                                                        viewModel.rateSolutionAndTutor(
                                                            questionId = questionId,
                                                            solutionId = s.id,
                                                            tutorId = s.tutorId,
                                                            rating = rating.toDouble(),
                                                            raterId = studentId
                                                        )
                                                    }
                                                },
                                                enabled = !alreadyRated,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (!alreadyRated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (!alreadyRated) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(if (alreadyRated) "Already Rated" else "Submit Rating")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm delete dialog
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    "Mark as Solved?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "This will delete the question and all its solutions permanently. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteQuestionWithSolutions(questionId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Status messages
    ratingStatus?.let { res ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRatingStatus() },
            title = {
                Text(
                    if (res.isSuccess) "Rating Submitted" else "Rating Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    res.exceptionOrNull()?.localizedMessage
                        ?: if (res.isSuccess) "Thank you for rating this solution!" else "Unable to submit rating. Please try again."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearRatingStatus() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    deleteStatus?.let { res ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearDeleteStatus()
                if (res.isSuccess) {
                    navController.navigate("student_dashboard") {
                        launchSingleTop = true
                    }
                }
            },
            title = {
                Text(
                    if (res.isSuccess) "Question Deleted" else "Delete Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    res.exceptionOrNull()?.localizedMessage
                        ?: if (res.isSuccess) "The question has been marked as solved and removed." else "Unable to delete. Please try again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearDeleteStatus()
                        if (res.isSuccess) {
                            navController.navigate("student_dashboard") {
                                launchSingleTop = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
