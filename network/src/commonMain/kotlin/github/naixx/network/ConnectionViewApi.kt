package github.naixx.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import github.naixx.network.internal.KtorfitClientProvider
import io.ktor.client.HttpClient

/**
 * The app's one [ViewApi]: the device's HTTP API at the address the connection found, read from
 * [WebSocketClient.connectionState] on every call (`Connected` and `LoginRequired` both carry it, so the login dialog
 * talks to the host that asked for credentials). Before that there is no address, and a call fails fast instead of
 * guessing a host. The auto-login inside [WebSocketClient] runs before any state exists and uses
 * [KtorfitClientProvider] directly.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConnectionViewApi(httpClient: HttpClient, private val connection: WebSocketClient) : ViewApi {

    private val apis = KtorfitClientProvider(httpClient)

    private val api: ViewApi
        get() {
            val state = connection.connectionState.value
            val url = when (state) {
                is ConnectionState.Connected -> state.address.fromUrl
                is ConnectionState.LoginRequired -> state.address.fromUrl
                else -> null
            }
            return apis.api(checkNotNull(url) { "No VIEW address: the connection is $state" })
        }

    override suspend fun auth(username: Any): Auth = api.auth(username)

    override suspend fun socketUrl(): AddressResponse = api.socketUrl()

    override suspend fun clips(): List<Clip> = api.clips()

    override suspend fun clipInfo(name: String): TimelapseClipInfo = api.clipInfo(name)

    override suspend fun download(file: String): ByteArray = api.download(file)

    override suspend fun socketUrl(url: String): AddressResponse = api.socketUrl(url)
}
