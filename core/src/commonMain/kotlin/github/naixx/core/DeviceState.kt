package github.naixx.core

import github.naixx.network.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass
import kotlin.time.Clock

/**
 * What the device last said, typed: one [StateFlow] per message class (null until the first message and again
 * after a disconnect), capture errors as a stream for the UI to toast and as [notifications] that stay until read,
 * and the device's clock. Owns the handshake: after every (re)connect it authenticates the session and asks for
 * the camera, settings, battery and program, so no shell has to know the protocol.
 *
 * Lives as long as the session (one per app / page), not as long as a screen: the Android service keeps the
 * connection while the UI is gone, and the state should still be there when the UI comes back.
 *
 * @param clock the client's clock (notification times, the device-clock offset); tests pass a fake.
 */
class DeviceState(
    private val session: DeviceSession,
    private val storage: StorageProvider,
    scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {

    val connection: StateFlow<ConnectionState> get() = session.state

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

    /** User-facing errors as they happen (capture failures); the shell shows each once (a toast). */
    val errors: SharedFlow<String>
        field = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Errors that stay until the user reads or clears them: a time-lapse runs for hours and a toast is easy to miss. */
    val notifications: StateFlow<List<AppNotification>>
        field = MutableStateFlow<List<AppNotification>>(emptyList())
    private var nextNotificationId = 0L

    val deviceClock = DeviceClock(clock)

    init {
        session.messages.onEach(::onMessage).launchIn(scope)
        session.state.onEach { state ->
            when (state) {
                is ConnectionState.Connected -> handshake()
                ConnectionState.Disconnected -> messageFlows.values.forEach { it.value = null }
                else -> Unit
            }
            if (state !is ConnectionState.Connected) deviceClock.onDisconnected()
        }.launchIn(scope)
    }

    fun send(message: OutMessage) = session.send(message)

    /** Mark every notification as read (the user opened the notification center). */
    fun markNotificationsRead() {
        notifications.update { list -> list.map { if (it.read) it else it.copy(read = true) } }
    }

    fun clearNotifications() {
        notifications.value = emptyList()
    }

    private fun handshake() {
        session.send(Session(storage.session() ?: ""))
        session.send(Get("camera"))
        session.send(Get("settings"))
        session.send(Get("battery"))
        session.send(Get("program"))
    }

    private fun onMessage(message: BaseMessage) {
        when (message) {
            is IntervalometerError -> {
                val text = message.msg ?: "An error occurred during capture"
                errors.tryEmit(text)
                notifications.update { it + AppNotification(nextNotificationId++, text, clock.now()) }
            }
            is CaptureError -> errors.tryEmit(message.message)
            is IntervalometerStatus -> deviceClock.onStatus(message.status.captureStartTime)
            else -> Unit
        }
        messageFlows.getOrPut(message::class) { MutableStateFlow(null) }.value = message
    }
}
