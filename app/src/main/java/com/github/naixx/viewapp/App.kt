package com.github.naixx.viewapp

import android.app.*
import com.github.naixx.viewapp.di.encodingModule
import github.naixx.network.*
import io.github.aakira.napier.*
import org.koin.android.ext.koin.*
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(networkModule, appModule, encodingModule)
        }
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
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

