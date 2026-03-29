package com.example.tutorHub.ui.theme.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutorHub.ViewModel.NotificationViewModel
import com.example.tutorHub.data.classes.Notification
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel = NotificationViewModel(),
    onNavigateToSessionRequest: (sessionId: String) -> Unit = {},
    onNavigateToSessionDetails: (sessionId: String) -> Unit = {}
) {
    val notifications by viewModel.notifications.observeAsState(emptyList())
    val unreadCount by viewModel.unreadCount.observeAsState(0)
    val loading by viewModel.loading.observeAsState(false)
    val error by viewModel.error.observeAsState(null)

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadNotifications(userId)
            viewModel.loadUnreadCount(userId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        NotificationsHeader(
            unreadCount = unreadCount,
            onMarkAllRead = { viewModel.markAllAsRead(userId) },
            onRefresh = { viewModel.loadNotifications(userId) }
        )

        // Error display
        if (!error.isNullOrEmpty()) {
            ErrorBanner(error!!, onDismiss = { viewModel.clearError() })
        }

        // Loading indicator
        if (loading && notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            // Empty state
            EmptyNotificationsState()
        } else {
            // Notifications list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { viewModel.markAsRead(notification.id) },
                        onDelete = { viewModel.deleteNotification(notification.id) },
                        onNavigateToSession = { sessionId ->
                            viewModel.markAsRead(notification.id)
                            when (notification.type) {
                                "session_requested", "reschedule_proposed" -> {
                                    onNavigateToSessionRequest(sessionId)
                                }
                                else -> onNavigateToSessionDetails(sessionId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsHeader(
    unreadCount: Int,
    onMarkAllRead: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Notifications",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (unreadCount > 0) {
                    Text(
                        text = "$unreadCount unread",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unreadCount > 0) {
                    IconButton(onClick = onMarkAllRead) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read",
                            tint = Color(0xFF6200EE)
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF6200EE)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToSession: (String) -> Unit
) {
    val notificationColor = when (notification.type) {
        "session_requested" -> Color(0xFFE3F2FD)
        "reschedule_proposed" -> Color(0xFFFFF3E0)
        "session_confirmed" -> Color(0xFFE8F5E9)
        else -> Color(0xFFF5F5F5)
    }

    val iconColor = when (notification.type) {
        "session_requested" -> Color(0xFF1976D2)
        "reschedule_proposed" -> Color(0xFFF57C00)
        "session_confirmed" -> Color(0xFF388E3C)
        else -> Color(0xFF666666)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable {
                if (!notification.read) {
                    onMarkAsRead()
                }
                if (!notification.sessionId.isNullOrEmpty()) {
                    onNavigateToSession(notification.sessionId!!)
                }
            },
        color = notificationColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Surface(
                modifier = Modifier
                    .size(40.dp),
                color = iconColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "session_requested" -> Icons.Default.EventNote
                        "reschedule_proposed" -> Icons.Default.Schedule
                        "session_confirmed" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = notification.type,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.read) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            color = Color(0xFF6200EE),
                            shape = RoundedCornerShape(4.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.body,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Additional details for reschedule notifications
                if (notification.type == "reschedule_proposed" && notification.proposedTime != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Proposed: ${formatDate(notification.proposedTime!!)}",
                            fontSize = 11.sp,
                            color = Color(0xFFF57C00),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Text(
                    text = formatTimeAgo(notification.createdAt),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete notification",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = "No notifications",
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Notifications",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You're all caught up!\nNotifications about session requests, reschedules, and confirmations will appear here.",
            fontSize = 14.sp,
            color = Color.LightGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = Color(0xFFFFEBEE),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color(0xFFC62828),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = error,
                fontSize = 12.sp,
                color = Color(0xFFC62828),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatDate(date: Date): String {
    return SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(date)
}

private fun formatTimeAgo(date: Date?): String {
    if (date == null) return "Just now"

    val now = System.currentTimeMillis()
    val diffInMillis = now - date.time
    val diffInSeconds = diffInMillis / 1000
    val diffInMinutes = diffInSeconds / 60
    val diffInHours = diffInMinutes / 60
    val diffInDays = diffInHours / 24

    return when {
        diffInSeconds < 60 -> "Just now"
        diffInMinutes < 60 -> "${diffInMinutes}m ago"
        diffInHours < 24 -> "${diffInHours}h ago"
        diffInDays < 7 -> "${diffInDays}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
