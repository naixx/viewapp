package github.naixx.core

import kotlin.time.Instant

/** A session-scoped notification (e.g. a capture error) shown in the in-app notification center. */
data class AppNotification(
    val id: Long,
    val message: String,
    val timeMillis: Instant,
    val read: Boolean = false,
)
