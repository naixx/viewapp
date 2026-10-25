package com.github.naixx.viewweb

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.ui.window.ComposeViewport
import dev.zacsweers.metro.*
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import github.naixx.core.*
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document

/**
 * The browser shell's graph: `:network` and `:core` arrive through `AppScope` contributions; `WebStorage` and
 * `DirectDeviceSession` contribute themselves; the stores are in memory — the page lives as long as the tab. It extends
 * [ViewModelGraph] for `:ui`'s `AppViewModelFactory` (`metroViewModelFactory`), which the page hands to the screens.
 */
@DependencyGraph(AppScope::class)
interface WebGraph : ViewModelGraph {

    val deviceSession: DeviceSession
    val deviceState: DeviceState
    val frameStore: FrameStore
}

@ContributesTo(AppScope::class)
@BindingContainer
object WebBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun frameStore(): FrameStore = MemoryFrameStore()

    @Provides
    @SingleIn(AppScope::class)
    fun clipRepository(): ClipRepository = MemoryClipRepository()
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())
    val graph = createGraph<WebGraph>()
    graph.deviceState
    graph.deviceSession.connect()
    val page = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }

    document.getElementById("loader")?.remove()
    ComposeViewport(document.body!!) { App(graph.metroViewModelFactory, graph.frameStore, page) }
}
