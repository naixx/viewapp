package com.github.naixx.viewweb

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.zacsweers.metro.*
import github.naixx.core.*
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document

/**
 * The browser shell's graph: `:network` and `:core` arrive through `AppScope` contributions; `WebStorage` and
 * `DirectDeviceSession` contribute themselves; the stores are in memory — the page lives as long as the tab.
 */
@DependencyGraph(AppScope::class)
interface WebGraph {

    val deviceSession: DeviceSession
    val deviceState: DeviceState
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
    val device = graph.deviceState // before connect(), so the handshake is observed
    graph.deviceSession.connect()

    document.getElementById("loader")?.remove()
    ComposeViewport(document.body!!) { App(device) }
}
