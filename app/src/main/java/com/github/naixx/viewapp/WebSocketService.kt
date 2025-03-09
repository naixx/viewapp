package com.github.naixx.viewapp

import android.app.Service
import android.content.Intent
import android.os.Binder
import androidx.core.app.*
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.utils.Prefs
import github.naixx.network.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.android.ext.android.inject

class WebSocketService : Service() {

    private val webSocketClient: WebSocketClient by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val messages = MutableSharedFlow<String>(1)
    private val storageProvider: StorageProvider by inject()
    val connectionState = webSocketClient.connectionState

    override fun onCreate() {
        super.onCreate()
        LL.e("onCreate")
        startForegroundService()
        startWebSocketConnection()
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "WebSocketChannel")
            .setContentTitle("WebSocket Service")
            .setContentText("Running WebSocket connection")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startWebSocketConnection() {
        val urls = Prefs.lastConnectedIp.toList() /*+ generateLocalServer(this)*/
        serviceScope.launch {
            webSocketClient.startWebSocket(urls, ::onSessionCreated) {
                LL.e(it)
            }
        }
        serviceScope.launch {
            webSocketClient.connectionState.collect {
                if (it is ConnectionState.LoginRequired) {
                    //delay(5000)
                    LL.e("login req")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private suspend fun onSessionCreated(
        session: DefaultClientWebSocketSession,
        state: ConnectionState.Connected
    ) {
        session.sendSerialized(Session(storageProvider.session() ?: ""))
        session.sendSerialized(Get("camera"))
        session.sendSerialized(Ping())
    }

    fun send(message: String) {
//        serviceScope.launch {
//
//            webSocketClient.send(message)
//        }
    }

    private fun handleMessage(message: String) {
        LL.e(message)
        //    messages.tryEmit(message)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        LL.e("onStartCommand")
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        LL.e("onstart")
    }

    override fun onDestroy() {
        super.onDestroy()
        LL.e("onDestroy")
        serviceScope.cancel()
        NotificationManagerCompat.from(this).cancelAll()
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent?) = binder
    inner class LocalBinder : Binder() {

        fun getService(): WebSocketService = this@WebSocketService
    }
}
