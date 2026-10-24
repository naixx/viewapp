package com.github.naixx.viewapp

import android.*
import android.R
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.*
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.network.holdViewAp
import com.github.naixx.viewapp.ui.MainActivity
import com.github.naixx.viewapp.utils.AppForeground
import com.github.naixx.viewapp.utils.Prefs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.android.ServiceKey
import github.naixx.core.DeviceSession
import github.naixx.core.DeviceState
import github.naixx.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The foreground anchor of the connection: it keeps the process alive while [AndroidDeviceSession] is connected
 * and does the Android-only work around it — the ongoing notification, holding the VIEW's hotspot and raising a
 * heads-up notification for capture errors while the app is in the background. The connection itself lives in
 * the session and the live-frame cache in DeviceState; this service starts and stops with the session.
 *
 * Built by the graph through MetroAppComponentFactory (see AppGraph), hence the constructor. [deviceState] is taken
 * here so the typed state (and with it the handshake after every connect) exists whenever the connection does, even
 * if the system restarted the service without the UI.
 */
@ServiceKey
@ContributesIntoMap(AppScope::class)
class WebSocketService(
    private val session: DeviceSession,
    private val deviceState: DeviceState,
) : Service() {

    companion object {

        const val ACTION_STOP_SERVICE = "com.github.naixx.viewapp.STOP_SERVICE"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, e -> LL.e(e) }
    private lateinit var serviceScope: CoroutineScope
    private var apJob: Job? = null
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    override fun onCreate() {
        super.onCreate()
        LL.e("onCreate; device ${deviceState.connection.value}")
        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
        serviceScope.launch {
            if (Prefs.apMode) setAccessPointMode(true) // the device was last reached on its hotspot
            session.state.collect {
                when (it) {
                    is ConnectionState.LoginRequired -> {
                        // The loop stops here; the Activity shows the login dialog and reconnects.
                        LL.e("login req")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }

                    is ConnectionState.Connecting -> sendNotification("Connecting to VIEW", R.drawable.ic_popup_sync)
                    is ConnectionState.Connected -> {
                        sendNotification("Connected to VIEW", R.drawable.ic_menu_gallery)
                        setAccessPointMode(it.address.fromUrl == ACCESS_POINT_URL)
                    }

                    else -> Unit
                }
            }
        }
        session.messages.onEach(::processMessage).launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            LL.e("Stopping from the notification action")
            session.disconnect() // stops this service too
            return START_NOT_STICKY
        }
        startForeground(1, createNotification("Not connected", R.drawable.ic_menu_gallery))
        session.connect() // idempotent; also covers a restart by the system
        return START_STICKY
    }

    private fun createNotification(text: String, iconRes: Int = R.drawable.ic_popup_sync): Notification {
        val stopIntent = Intent(this, WebSocketService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "WebSocketChannel")
            .setContentTitle("WebSocket Service")
            .setContentText(text)
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    /**
     * On the VIEW's hotspot keep the phone there ([holdViewAp]); on a shared Wi-Fi let it go.
     * Only called from the connection-state collector, so [apJob] needs no locking; cancelling
     * [serviceScope] (Stop, onDestroy) releases the hotspot too.
     */
    private fun setAccessPointMode(on: Boolean) {
        Prefs.apMode = on
        if (!on) {
            apJob?.cancel()
            apJob = null
        } else if (apJob?.isActive != true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            apJob = serviceScope.launch { getSystemService(ConnectivityManager::class.java).holdViewAp() }
        }
    }

    private fun sendNotification(text: String, icon: Int) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return
        notificationManager.notify(1, createNotification(text, icon))
    }

    private fun processMessage(message: BaseMessage) {
        if (message is IntervalometerError && !AppForeground.isForeground)
            notifyError(message.msg ?: "An error occurred during capture")
    }

    private var errorNotificationId = 1000

    /** Post a heads-up system notification for a capture error (only while the app is backgrounded). */
    private fun notifyError(text: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(this, 1, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "ErrorsChannel")
            .setContentTitle("Time-lapse error")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()
        notificationManager.notify(errorNotificationId++, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        LL.e("onDestroy")
        serviceScope.cancel()
        apJob = null
        notificationManager.cancelAll()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
