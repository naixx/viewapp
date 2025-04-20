package com.github.naixx.viewapp.ui

import androidx.lifecycle.*
import com.github.naixx.logger.LL
import github.naixx.network.*
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass

class MainViewModel : ViewModel(), KoinComponent {

    val outgoing = MutableSharedFlow<OutMessage>(1)

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val clips = MutableStateFlow<ImmutableList<Clip>>(persistentListOf())

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
        val flow = messageFlows.getOrPut(message::class) {
            MutableStateFlow(null)
        }

        flow.value = message
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
