package com.github.naixx.viewweb

import github.naixx.network.*
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * The browser's counterpart of the Android WebSocketService + MainViewModel: owns the shared [WebSocketClient]
 * loop and fans the device's messages into one state flow per message class. The page comes from the VIEW, so
 * the device is at the page's own origin: one candidate, no subnet scan, and the client's reconnect loop covers
 * a tab that iOS froze and thawed. On every (re)connect the device is asked for its state again.
 */
class Connection(private val client: WebSocketClient, private val storage: StorageProvider) {

    val state: StateFlow<ConnectionState> get() = client.connectionState

    private val messageFlows = mutableMapOf<KClass<out BaseMessage>, MutableStateFlow<BaseMessage?>>()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : BaseMessage> messageFlow(): StateFlow<T?> =
        messageFlows.getOrPut(T::class) { MutableStateFlow(null) } as StateFlow<T?>

    val camera get() = messageFlow<ConnectedMessage>()
    val settings get() = messageFlow<SettingsMessage>()
    val battery get() = messageFlow<Battery>()
    val thumbnail get() = messageFlow<Thumbnail>()
    val intervalometerStatus get() = messageFlow<IntervalometerStatus>()
    val histogram get() = messageFlow<Histogram>()
    val program get() = messageFlow<TimelapseProgram>()

    /** The last capture / intervalometer error, until the user dismisses it (a time-lapse runs for hours). */
    val lastError = MutableStateFlow<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            client.startWebSocket(
                priorityUrls = listOf(window.location.origin + "/"),
                scanUrls = emptyList(),
                onCreate = { session, _ ->
                    // Same handshake as the Android service.
                    session.sendSerialized(Session(storage.session() ?: ""))
                    session.sendSerialized(Get("camera"))
                    session.sendSerialized(Get("settings"))
                    session.sendSerialized(Get("battery"))
                    session.sendSerialized(Get("program"))
                },
                onMessage = ::onMessage,
            )
        }
    }

    fun send(message: OutMessage) {
        scope.launch { client.send(message) }
    }

    fun clearError() {
        lastError.value = null
    }

    private fun onMessage(message: BaseMessage) {
        when (message) {
            is IntervalometerError -> lastError.value = message.msg ?: "An error occurred during capture"
            is CaptureError -> lastError.value = message.message
            else -> Unit
        }
        messageFlows.getOrPut(message::class) { MutableStateFlow(null) }.value = message
    }
}
