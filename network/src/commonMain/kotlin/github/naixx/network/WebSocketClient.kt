package github.naixx.network

import com.github.naixx.logger.LL
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import github.naixx.network.AddressResponse.Address
import github.naixx.network.internal.KtorfitClientProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.get
import io.ktor.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

const val ACCESS_POINT_URL = "http://10.0.0.1/"
const val REMOTE_URL = "https://app.view.tl/"
val WIFI_URL = "http://192.168.31.21/"
private const val RECONNECT_DELAY_MS = 3000L

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class LoginRequired(val address: AddressResponse.LoginRequired) : ConnectionState
    data class Connected(val address: Address, val isLocal: Boolean) : ConnectionState
}

internal fun handshake(session: String?): List<OutMessage> =
    listOf(Session(session ?: ""), Get("camera"), Get("settings"), Get("battery"), Get("program"))

@Inject
@SingleIn(AppScope::class)
class WebSocketClient(
    private val json: Json,
    private val client: HttpClient,
    private val storageProvider: StorageProvider,
) {

    // Auto-login talks to the host that just answered the probe, before any connection state exists.
    private val apis = KtorfitClientProvider(client)

    private var session: DefaultClientWebSocketSession? = null

    val connectionState: StateFlow<ConnectionState>
        field = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    val messages: SharedFlow<BaseMessage>
        field = MutableSharedFlow<BaseMessage>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    suspend fun startWebSocket(priorityUrls: List<String>, scanUrls: List<String>) {
        try {
            reconnect(priorityUrls, scanUrls)
        } catch (e: CancellationException) {
            connectionState.value = ConnectionState.Disconnected
            throw e
        }
    }

    private suspend fun reconnect(priorityUrls: List<String>, scanUrls: List<String>) {
        var useLocalConnection = true

        while (true) {
            try {
                connectionState.value = ConnectionState.Connecting
                // Get the connection URL based on current preference
                val foundServer = findServer(useLocalConnection, priorityUrls, scanUrls)

                if (foundServer == null) {
//                    TODO useLocalConnection = !useLocalConnection
                    println("switching to local connection $useLocalConnection")
                    if (useLocalConnection)
                        delay(RECONNECT_DELAY_MS)
                    continue
                }


                when (foundServer) {
                    is Address                       -> {
                        foundServer.fromUrl?.let {
                            storageProvider.lastSuccessfulAddress(it)
                        }
                        actualConnect(foundServer)
                    }

                    is AddressResponse.LoginRequired -> {
                        val api = apis.api(foundServer.fromUrl ?: REMOTE_URL)
                        try {
                            // Get credentials from StorageProvider
                            val email = storageProvider.email()!!
                            val password = storageProvider.password()!!
                            val auth = api.auth(mapOf("email" to email, "password" to password))
                            storageProvider.session(auth.session)
                            val address = api.socketUrl(foundServer.fromUrl + "socket/address")
                            if (address is AddressResponse.Address)
                                actualConnect(address)
                            else {
                                connectionState.value = ConnectionState.LoginRequired(foundServer)
                                break
                            }
                        } catch (e: Exception) {
                            print(e)
                            connectionState.value = ConnectionState.LoginRequired(foundServer)
                            break
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Explicit stop / scope cancellation — let it tear the loop down cleanly.
                throw e
            } catch (e: Exception) {
                LL.e(e)
                // A dropped connection (Wi-Fi blip, device sleep) is transient: keep the loop alive
                // and stay in Connecting so we transparently re-discover and reconnect. We must NOT
                // emit Disconnected here — the UI treats that as a teardown signal and unbinds.
                connectionState.value = ConnectionState.Connecting
                session = null
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun actualConnect(foundServer: Address) {
        client.webSocket(foundServer.address) {
            handshake(storageProvider.session()).forEach { sendSerialized(it) }
            session = this
            connectionState.value = ConnectionState.Connected(foundServer, true)

            launch {
                try {
                    while (isActive) {
                        sendSerialized(Ping())
                        delay(3000)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The socket aborted (e.g. Wi-Fi switch). Don't let this unguarded coroutine
                    // crash the app — the receive loop below detects the dead socket and the outer
                    // reconnect loop takes over.
                    LL.e(e)
                }
            }
            while (isActive) {
                try {
                    messages.tryEmit(receiveDeserialized<BaseMessage>())
                } catch (e: kotlinx.coroutines.channels.ClosedReceiveChannelException) {
                    LL.e(e)
                    throw e
                } catch (e: Exception) {
                    LL.e(e)
                }
            }
        }
    }

    private suspend fun findServer(
        useLocalConnection: Boolean,
        priorityUrls: List<String>,
        scanUrls: List<String>,
    ): AddressResponse? =
        if (useLocalConnection) {
            // Always scan. scanHosts probes the last-known-good addresses in parallel with the
            // subnet, so this stays fast when the device hasn't moved and still re-discovers it
            // after a Wi-Fi switch. No address cache: a stale cached IP would otherwise pin us to
            // a host that no longer exists on the new network.
            client.scanHosts(priorityUrls, scanUrls)
        } else {
            runCatching {
                val remoteAddress = client.get(REMOTE_URL + "socket/address").body<AddressResponse>()
                remoteAddress.apply { fromUrl = REMOTE_URL }
            }.getOrNull()
        }

    suspend fun send(message: OutMessage) {
        session?.sendSerialized(message)
    }
}
