package com.github.naixx.viewweb

import com.github.naixx.logger.LL
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import github.naixx.core.DeviceSession
import github.naixx.network.*
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The browser's [DeviceSession]: the shared [WebSocketClient] loop running in the page. The page is served by the
 * VIEW, so the device is at the page's own origin — one candidate, no subnet scan — and the client's reconnect
 * loop covers a tab that iOS froze and thawed. Nothing to start or stop for the user: the page is the connection.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DirectDeviceSession(private val client: WebSocketClient) : DeviceSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var connectionJob: Job? = null

    override val state: StateFlow<ConnectionState> get() = client.connectionState

    override val messages = MutableSharedFlow<BaseMessage>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val controllable = false

    override val subnetLabel: StateFlow<String?> = MutableStateFlow(null) // no scan: the page's origin is the device

    override fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch {
            // The handshake after a connect is DeviceState's.
            client.startWebSocket(listOf(window.location.origin + "/"), emptyList(), { _, _ -> }) { message ->
                if (!messages.tryEmit(message)) LL.w("message dropped: ${message.type}")
            }
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        client.connectionState.value = ConnectionState.Disconnected
    }

    override fun send(message: OutMessage) {
        scope.launch { client.send(message) }
    }
}
