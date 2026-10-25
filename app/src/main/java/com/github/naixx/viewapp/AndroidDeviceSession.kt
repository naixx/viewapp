package com.github.naixx.viewapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import androidx.core.content.ContextCompat
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.network.currentWifiSubnetLabel
import com.github.naixx.viewapp.network.generateLocalServer
import com.github.naixx.viewapp.utils.Prefs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import github.naixx.core.DeviceSession
import github.naixx.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidDeviceSession(
    private val context: Context,
    private val client: WebSocketClient,
) : DeviceSession {

    private val exceptionHandler = CoroutineExceptionHandler { _, e -> LL.e(e) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private val service get() = Intent(context, WebSocketService::class.java)

    override val state: StateFlow<ConnectionState> get() = client.connectionState

    override val messages: SharedFlow<BaseMessage> get() = client.messages

    override val controllable = true

    /** The phone's Wi-Fi subnet (what [connect] scans), refreshed on every default-network change while someone looks. */
    override val subnetLabel: StateFlow<String?> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun refresh() { trySend(currentWifiSubnetLabel(context)) }
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = refresh()
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(), currentWifiSubnetLabel(context))

    override fun connect() {
        ContextCompat.startForegroundService(context, service)
    }

    override fun disconnect() {
        context.stopService(service)
    }

    override fun send(message: OutMessage) {
        scope.launch { client.send(message) }
    }

    suspend fun keepConnected() {
        val priorityUrls = Prefs.lastConnectedIp.toList()
        val scanUrls = generateLocalServer(context) + WIFI_URL
        LL.i("connecting; known: ${priorityUrls.joinToString()}, scanning ${scanUrls.size} hosts")
        client.startWebSocket(priorityUrls, scanUrls)
    }
}
