package com.github.naixx.viewapp.ui

import androidx.lifecycle.*
import com.github.naixx.logger.LL
import github.naixx.network.*
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf

class MainViewModel : ViewModel(), KoinComponent {

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val clips = MutableStateFlow<ImmutableList<Clip>>(persistentListOf())

    val connectedMessage = MutableStateFlow<ConnectedMessage?>(null)
    val settingsMessage = MutableStateFlow<SettingsMessage?>(null)
    val batteryMessage = MutableStateFlow<Battery?>(null)

    init {
        connectionState.onEach {
            if (it is ConnectionState.Disconnected) {
                connectedMessage.value = null
                settingsMessage.value = null
                batteryMessage.value = null
            }
        }.launchIn(viewModelScope)
    }

    fun onMessage(message: BaseMessage) {
        when (message) {
            is ConnectedMessage -> connectedMessage.value = message
            is SettingsMessage  -> settingsMessage.value = message
            is Battery          -> batteryMessage.value = message
            else                -> Unit
        }
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
