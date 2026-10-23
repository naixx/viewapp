package com.github.naixx.viewweb

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import github.naixx.network.StorageProvider
import github.naixx.network.networkModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val webModule = module {
    single<StorageProvider> { WebStorage() }
    singleOf(::Connection)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())
    val koin = startKoin { modules(networkModule, webModule) }.koin
    val connection = koin.get<Connection>()
    connection.start()

    document.getElementById("loader")?.remove()
    ComposeViewport(document.body!!) { App(connection) }
}
