package com.github.naixx.viewapp.utils

/**
 * Whether the app UI is currently in the foreground. Set from `MainActivity` start/stop and read by
 * `WebSocketService` to decide whether to post a system notification (only when backgrounded — the
 * in-app notification center + toast already cover the foreground case).
 */
object AppForeground {
    @Volatile
    var isForeground: Boolean = false
}
