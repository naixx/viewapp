package com.github.naixx.viewapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.naixx.core.AppNotification
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun NotificationBell(
    notifications: List<AppNotification>,
    onRead: () -> Unit,
    onClear: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val unread = notifications.count { !it.read }
    Box {
        BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        }
        // Mark read only when the center closes, so new (unread) items stay highlighted while open.
        DropdownMenu(expanded = open, onDismissRequest = { open = false; onRead() }) {
            NotificationList(notifications, onItem = { open = false; onRead() }, onClear = { onClear(); open = false })
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<AppNotification>,
    onItem: () -> Unit,
    onClear: () -> Unit,
) {
    if (notifications.isEmpty()) {
        DropdownMenuItem(text = { Text("No notifications") }, enabled = false, onClick = {})
    } else {
        // Newest first; unread items show a red dot and bold text.
        notifications.asReversed().forEach { n ->
            val unreadDot: (@Composable () -> Unit)? = if (n.read) null else {
                { Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error)) }
            }
            DropdownMenuItem(
                leadingIcon = unreadDot,
                text = {
                    Text(
                        n.timeMillis.toLocalDateTime(TimeZone.currentSystemDefault()).toString().replace('T', ' ').substring(0, 19)
                                + " " + n.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold,
                    )
                },
                onClick = onItem,
            )
            HorizontalDivider()
        }
        DropdownMenuItem(text = { Text("Clear all") }, onClick = onClear)
    }
}

@Preview
@Composable
fun PreviewNotificationBell() {

    NotificationBell(listOf(AppNotification(2, "asdsad", Clock.System.now())), {}, {})
}

@Preview
@Composable
fun PreviewContentList() {

    NotificationList(listOf(AppNotification(2, "asdsad", Clock.System.now())), {}, {})
}
