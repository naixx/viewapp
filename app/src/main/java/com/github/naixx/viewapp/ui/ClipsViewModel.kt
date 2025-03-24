package com.github.naixx.viewapp.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.github.naixx.logger.LL
import github.naixx.db.ClipRepository
import github.naixx.network.ConnectionState
import github.naixx.network.ViewApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ClipsViewModel : ScreenModel, KoinComponent {
    private val clipRepository: ClipRepository by inject()

    val clips = clipRepository.getAllClips().stateIn(
        screenModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun refreshClips(connection: ConnectionState.Connected) {
        screenModelScope.launch {
            try {
                val viewApi: ViewApi = inject<ViewApi> { parametersOf(connection.address.fromUrl) }.value
                clipRepository.refreshClips(viewApi)
            } catch (e: Exception) {
                LL.e(e)
            }
        }
    }
}
