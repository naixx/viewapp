package com.github.naixx.viewweb

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import github.naixx.core.*
import github.naixx.network.StorageProvider
import github.naixx.network.networkModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** The browser shell's bindings for the :core contracts: no files, no database — the page lives as long as the tab. */
val webModule = module {
    single<StorageProvider> { WebStorage() }
    single<DeviceSession> { DirectDeviceSession(get()) }
    single<FrameStore> { MemoryFrameStore() }
    single<ClipRepository> { MemoryClipRepository() }
    single { DeviceState(get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())
    val koin = startKoin { modules(networkModule, webModule) }.koin
    val device = koin.get<DeviceState>() // before connect(), so the handshake is observed
    koin.get<DeviceSession>().connect()

    document.getElementById("loader")?.remove()
    ComposeViewport(document.body!!) { App(device) }
}
