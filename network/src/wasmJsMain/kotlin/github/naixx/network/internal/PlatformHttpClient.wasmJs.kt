package github.naixx.network.internal

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

// The browser's fetch and WebSocket; request logging is the browser's network panel.
internal actual fun platformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Js) {
    block()
}
