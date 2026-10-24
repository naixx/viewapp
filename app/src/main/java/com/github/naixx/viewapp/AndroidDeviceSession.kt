package com.github.naixx.viewapp

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.network.generateLocalServer
import com.github.naixx.viewapp.utils.Prefs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import github.naixx.core.DeviceSession
import github.naixx.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Android [DeviceSession]: owns the shared [WebSocketClient] and its reconnect loop for the life of the
 * process (a single in the app graph), and keeps [WebSocketService] running in the foreground while connected so the system
 * leaves the socket alone when the UI is gone. The UI and the service both talk to this object; nothing is bound.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidDeviceSession(
    private val context: Context,
    private val client: WebSocketClient,
) : DeviceSession {

    // Safety net: socket aborts during a network switch can surface as uncaught exceptions on engine/teardown
    // coroutines. Without a handler they reach the thread's default handler and crash the app; here we just log
    // and let the reconnect loop recover.
    private val exceptionHandler = CoroutineExceptionHandler { _, e -> LL.e(e) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private var connectionJob: Job? = null

    override val state: StateFlow<ConnectionState> get() = client.connectionState

    // Emitted from the receive loop without suspending, so the order is kept; the buffer only matters if the
    // collector (DeviceState) stalls.
    override val messages = MutableSharedFlow<BaseMessage>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val controllable = true

    /**
     * Idempotent: called from the Activity, the login dialog and the service's own start (also when the system
     * restarts it). Without the guard each call would launch another infinite reconnect loop, and after one
     * connected the others would keep scanning and fight over the single WebSocket session.
     */
    override fun connect() {
        if (connectionJob?.isActive != true) {
            val priorityUrls = Prefs.lastConnectedIp.toList()
            val scanUrls = generateLocalServer(context) + WIFI_URL
            LL.i("connecting; known: ${priorityUrls.joinToString()}, scanning ${scanUrls.size} hosts")
            connectionJob = scope.launch {
                // The handshake after a connect is DeviceState's.
                client.startWebSocket(priorityUrls, scanUrls, { _, _ -> }) { message ->
                    if (!messages.tryEmit(message)) LL.w("message dropped: ${message.type}")
                }
            }
        }
        ContextCompat.startForegroundService(context, Intent(context, WebSocketService::class.java))
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        client.connectionState.value = ConnectionState.Disconnected
        context.stopService(Intent(context, WebSocketService::class.java))
    }

    override fun send(message: OutMessage) {
        scope.launch { client.send(message) }
    }
}
