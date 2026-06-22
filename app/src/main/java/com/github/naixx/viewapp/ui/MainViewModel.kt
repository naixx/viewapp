package com.github.naixx.viewapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.utils.EventBus
import github.naixx.network.BaseMessage
import github.naixx.network.Battery
import github.naixx.network.Clip
import github.naixx.network.ConnectedMessage
import github.naixx.network.ConnectionState
import github.naixx.network.Histogram
import github.naixx.network.IntervalometerError
import github.naixx.network.IntervalometerStatus
import github.naixx.network.OutMessage
import github.naixx.network.SettingsMessage
import github.naixx.network.Thumbnail
import github.naixx.network.TimelapseProgram
import github.naixx.network.ViewApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Instant

/** A session-scoped notification (e.g. a capture error) shown in the in-app notification center. */
data class AppNotification(
    val id: Long,
    val message: String,
    val timeMillis: Instant,
    val read: Boolean = false,
)

class MainViewModel : ViewModel(), KoinComponent {

    val outgoing = MutableSharedFlow<OutMessage>(1)

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val clips = MutableStateFlow<ImmutableList<Clip>>(persistentListOf())

    // Persistent (session-lived) notifications — errors stay until the user reads/clears them, since a
    // time-lapse runs for a long time and the toast is easy to miss.
    val notifications: StateFlow<List<AppNotification>>
        field = MutableStateFlow<List<AppNotification>>(emptyList())
    private var nextNotificationId = 0L

    private val messageFlows = mutableMapOf<KClass<out BaseMessage>, MutableStateFlow<BaseMessage?>>()

    private inline fun <reified T : BaseMessage> messageFlow(): StateFlow<T?> =
        messageFlows.getOrPut(T::class) { MutableStateFlow(null) } as StateFlow<T?>

    val connectedMessage get() = messageFlow<ConnectedMessage>()
    val settingsMessage get() = messageFlow<SettingsMessage>()
    val batteryMessage get() = messageFlow<Battery>()
    val thumbnail get() = messageFlow<Thumbnail>()
    val intervalometerStatus get() = messageFlow<IntervalometerStatus>()
    val histogram get() = messageFlow<Histogram>()
    val program get() = messageFlow<TimelapseProgram>()

    init {
        connectionState.onEach {
            if (it is ConnectionState.Disconnected) {
                messageFlows.values.forEach { flow -> flow.value = null }
            }
        }.launchIn(viewModelScope)
    }

    fun onMessage(message: BaseMessage) {
        if (message is IntervalometerError) {
            val text = message.msg ?: "An error occurred during capture"
            EventBus.error(text)
            addNotification(text)
        }

        val flow = messageFlows.getOrPut(message::class) {
            MutableStateFlow(null)
        }

        flow.value = message
    }

    private fun addNotification(message: String) {
        notifications.update { it + AppNotification(nextNotificationId++, message, Clock.System.now()) }
    }

    /** Mark every notification as read (called when the user opens the notification center). */
    fun markNotificationsRead() {
        notifications.update { it.map { if (it.read) it else it.copy(read = true) } }
    }

    fun clearNotifications() {
        notifications.value = emptyList()
    }

    fun send(message: OutMessage) {
        outgoing.tryEmit(message)
    }

    /**
     * Request the list of available clips from the server
     */
    fun requestClips(connection: ConnectionState.Connected) {
        viewModelScope.launch {
            try {
                val viewApi: ViewApi = get<ViewApi> { parametersOf(connection.address.fromUrl) }
                clips.value = viewApi.clips().toImmutableList()
            } catch (e: Exception) {
                LL.e(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.coroutineContext.cancelChildren()
    }
}
