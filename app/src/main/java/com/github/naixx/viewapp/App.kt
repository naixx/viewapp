package com.github.naixx.viewapp

import android.app.*
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication
import io.github.aakira.napier.*

class App : Application(), MetroApplication {

    /**
     * The dependency graph; built on first use (docs/2026-10-24-shared-ui-architecture.md, step 8). Nothing reaches it
     * directly: the Activity, the service and the view models are constructed by it (see AppGraph).
     */
    private val graph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    /** ViewAppComponentFactory (manifest) asks here for the Activity and the service. */
    override val appComponentProviders: MetroAppComponentProviders get() = graph

    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "WebSocketChannel",
            "WebSocket Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.setShowBadge(true)
//            channel.enableLights(true)
//            channel.enableVibration(true)

        // Separate high-importance channel for time-lapse errors (heads-up + sound/vibration), used
        // by WebSocketService to alert the user while the app is backgrounded.
        val errorsChannel = NotificationChannel(
            "ErrorsChannel",
            "Time-lapse Errors",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(true)
            enableVibration(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        manager?.createNotificationChannel(errorsChannel)
    }
}
