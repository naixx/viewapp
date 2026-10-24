package com.github.naixx.viewapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.naixx.logger.LL
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import github.naixx.core.ClipRepository
import github.naixx.network.ViewApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Clips tab's list. A `ViewModel` like the others (it used to be a Voyager `ScreenModel`, which only Koin could
 * build), taken with `screenViewModel()`: it lives and dies with the Clips screen, as the `ScreenModel` did.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ClipsViewModel(
    private val clipRepository: ClipRepository,
    private val api: ViewApi,
) : ViewModel() {

    val clips = clipRepository.getAllClips().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    /** Pull the device's clip list; call it once connected (the api follows the connection). */
    fun refreshClips() {
        viewModelScope.launch {
            try {
                clipRepository.refreshClips(api)
            } catch (e: Exception) {
                LL.e(e)
            }
        }
    }
}
