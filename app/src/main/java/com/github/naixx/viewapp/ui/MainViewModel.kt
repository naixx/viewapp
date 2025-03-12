package com.github.naixx.viewapp.ui

import androidx.lifecycle.*
import com.github.naixx.logger.LL
import github.naixx.network.*
import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf

class MainViewModel : ViewModel(), KoinComponent {

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val clips = MutableStateFlow<ImmutableList<Clip>>(persistentListOf())

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
}
