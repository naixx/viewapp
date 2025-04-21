package com.github.naixx.viewapp

import android.app.*
import android.content.Intent
import android.os.Binder
import android.util.Base64
import androidx.core.app.*
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.network.generateLocalServer
import com.github.naixx.viewapp.utils.Prefs
import github.naixx.network.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.android.ext.android.inject
import java.io.File

class WebSocketService : Service() {

    companion object {

        const val ACTION_STOP_SERVICE = "com.github.naixx.viewapp.STOP_SERVICE"
    }

    private val webSocketClient: WebSocketClient by inject()
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val messages = MutableSharedFlow<BaseMessage>(1)
    private val storageProvider: StorageProvider by inject()
    val connectionState = webSocketClient.connectionState

    override fun onCreate() {
        super.onCreate()
        LL.e("onCreate")
    }

    private fun startForegroundService() {
        val stopIntent = Intent(this, WebSocketService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "WebSocketChannel")
            .setContentTitle("WebSocket Service")
            .setContentText("Running WebSocket connection")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startWebSocketConnection() {
        if (!serviceScope.isActive)
            serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val urls = Prefs.lastConnectedIp.toList() + generateLocalServer(this) + WIFI_URL
        LL.e(urls.joinToString("\n"))
        serviceScope.launch {
            webSocketClient.startWebSocket(urls, ::onSessionCreated) {
                LL.i(it)
                launch {
                    messages.emit(it)
                }
                processMessage(it)
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
        session.sendSerialized(Get("settings"))
        session.sendSerialized(Get("battery"))
        session.sendSerialized(Get("program"))
    }

    fun send(message: OutMessage) {
        serviceScope.launch {
            webSocketClient.send(message)
        }
    }

    private fun processMessage(message: BaseMessage) {
        if (message is Thumbnail) {
            saveThumbnail(message)
        }
    }

    private fun saveThumbnail(thumbnail: Thumbnail) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val timelapseDir = File(filesDir, "timelapses/${thumbnail.tlName.lowercase()}").apply { mkdirs() }
                val frameNumber = thumbnail.frameIndex
                val frameFile = File(timelapseDir, "frame_$frameNumber.jpg")

                val imageData = Base64.decode(thumbnail.imageBase64, Base64.DEFAULT)
                frameFile.writeBytes(imageData)

                LL.d("Service saved thumbnail: ${frameFile.absolutePath}")
            } catch (e: Exception) {
                LL.e("Error saving thumbnail: ${e.message}")
            }
        }
    }

    private fun handleMessage(message: String) {
        LL.e(message)
        //    messages.tryEmit(message)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            LL.e("Stopping service from notification action")
            connectionState.value = ConnectionState.Disconnected
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            serviceScope.cancel()
            return START_NOT_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
        LL.e("onStartCommand")
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        LL.e("onstart")
        startForegroundService()
        startWebSocketConnection()
        // onStartCommand(intent, flags, startId)
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
