package com.github.naixx.viewweb

import github.naixx.network.*
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The browser's counterpart of the Android WebSocketService: owns the shared [WebSocketClient] loop and fans the
 * messages the UI shows into state flows. The page comes from the VIEW, so the device is at the page's own origin:
 * one candidate, no subnet scan, and the client's reconnect loop covers a tab that iOS froze and thawed.
 */
class Connection(private val client: WebSocketClient, private val storage: StorageProvider) {

    val state: StateFlow<ConnectionState> get() = client.connectionState
    val battery = MutableStateFlow<Battery?>(null)
    val camera = MutableStateFlow<ConnectedMessage?>(null)
    val received = MutableStateFlow(0)
    val lastType = MutableStateFlow<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            client.startWebSocket(
                priorityUrls = listOf(window.location.origin + "/"),
                scanUrls = emptyList(),
                onCreate = { session, _ ->
                    session.sendSerialized(Session(storage.session() ?: ""))
                    session.sendSerialized(Get("camera"))
                    session.sendSerialized(Get("battery"))
                },
                onMessage = ::onMessage,
            )
        }
    }

    fun send(message: OutMessage) {
        scope.launch { client.send(message) }
    }

    private fun onMessage(message: BaseMessage) {
        received.value += 1
        lastType.value = message.type
        when (message) {
            is Battery -> battery.value = message
            is ConnectedMessage -> camera.value = message
            else -> Unit
        }
    }
}
