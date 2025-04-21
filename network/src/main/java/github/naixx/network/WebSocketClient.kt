package github.naixx.network

import com.github.naixx.logger.LL
import github.naixx.network.AddressResponse.Address
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.get
import io.ktor.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf

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

class WebSocketClient(
    private val json: Json,
    private val client: HttpClient,
    private val storageProvider: StorageProvider,
) : KoinComponent {

    var session: DefaultClientWebSocketSession? = null
    private var cachedLocalAddress: AddressResponse? = null
    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    suspend fun startWebSocket(
        localUrls: List<String>,
        onCreate: suspend (DefaultClientWebSocketSession, ConnectionState.Connected) -> Unit,
        onMessage: (BaseMessage) -> Unit
    ) {
        var useLocalConnection = true

        while (true) {
            try {
                connectionState.value = ConnectionState.Connecting
                // Get the connection URL based on current preference
                val foundServer = findServer(useLocalConnection, localUrls)

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
                        actualConnect(foundServer, onCreate, onMessage)
                    }

                    is AddressResponse.LoginRequired -> {
                        val api = get<ViewApi>(parameters = { parametersOf(foundServer.fromUrl) })
                        try {
                            // Get credentials from StorageProvider
                            val email = storageProvider.email()!!
                            val password = storageProvider.password()!!
                            val auth = api.auth(mapOf("email" to email, "password" to password))
                            storageProvider.session(auth.session)
                            val address = api.socketUrl(foundServer.fromUrl + "socket/address")
                            if (address is AddressResponse.Address)
                                actualConnect(address, onCreate, onMessage)
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
            } catch (e: Exception) {
                LL.e(e)
                connectionState.value = ConnectionState.Disconnected
                session = null
                // Switch connection type for next attempt
//              TODO  useLocalConnection = !useLocalConnection
                // Add delay before reconnecting
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun actualConnect(
        foundServer: Address,
        onCreate: suspend (DefaultClientWebSocketSession, ConnectionState.Connected) -> Unit,
        onMessage: (BaseMessage) -> Unit
    ) {
        client.webSocket(foundServer.address) {
            session = this
            val newState = ConnectionState.Connected(foundServer, true/*TODO*/)
            connectionState.value = newState
            onCreate(this, newState)

            launch {
                while (isActive) {
                    sendSerialized(Ping())
                    delay(3000)
                }
            }
            while (isActive) {
                try {
                    val message = receiveDeserialized<BaseMessage>()
                    onMessage(message)
                } catch (e: kotlinx.coroutines.channels.ClosedReceiveChannelException) {
                    LL.e(e)
                    throw e
                } catch (e: Exception) {
                    LL.e(e)
                }
            }
        }
    }

    private suspend fun findServer(useLocalConnection: Boolean, localUrls: List<String>): AddressResponse? =
        if (useLocalConnection) {
            // Try to use cached local address or scan for one
            if (cachedLocalAddress == null) {
                cachedLocalAddress = client.scanHosts(localUrls) //todo start with local??
            }
            cachedLocalAddress?.let { localAddress ->
                runCatching {
                    val local = client.get(localAddress.fromUrl + "socket/address").body<AddressResponse>()
                    local.apply { fromUrl = localAddress.fromUrl }
                }.getOrNull()
            }
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
