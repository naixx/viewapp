package com.github.naixx.viewapp.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.*
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.ClipMapper
import github.naixx.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {

    val flow = MutableStateFlow(emptyList<String>())
    val out = MutableSharedFlow<String>(1)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            LL.e("Notification permission granted")
        } else {
            LL.e("Notification permission denied")
        }
    }
    private val storageProvider: StorageProvider by inject()
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
        val intent = Intent(this, WebSocketService::class.java)
        startForegroundService(intent)
        bind()
        LL.e(viewModel)
        setContent {

            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components {
                        add(KtorNetworkFetcherFactory())
                        add(ClipMapper())
                    }
                    .crossfade(100)
                    .logger(DebugLogger())
                    .build()
            }
            ViewAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by flow.collectAsState()
                    val conn by viewModel.connectionState.collectAsState()

                    LL.e(conn)
                    MainScreen(conn, state, modifier = Modifier.padding(innerPadding)) {
                        // startService()
                    }
                    val c = conn
                    if (c is ConnectionState.LoginRequired) {
                        LoginDialog(
                            onLogin = { username, password ->
                                apiLogin(c, username, password)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun apiLogin(conn: ConnectionState.LoginRequired, username: String, password: String) {
        LL.e("Login attempt with $username")
        lifecycleScope.launch {
            try {
                val viewApi: ViewApi = get<ViewApi>(parameters = { parametersOf(conn.address.fromUrl) })
                val auth = viewApi.auth(mapOf("email" to username, "password" to password))
                storageProvider.session(auth.session)
                LL.e("Login successful, session: ${auth.session}")
                startService()
            } catch (e: Exception) {
                LL.e("Login failed: ${e.message}")
                //TODO show toast
            }
        }
    }

    private fun startService() {
        val intent = Intent(this, WebSocketService::class.java)
        startService(intent)
        bind()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    LL.e("Notification permission already granted")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        var isBound = false
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            LL.e(name)
            val service = (binder as WebSocketService.LocalBinder).getService()
            isBound = true
            service.messages.onEach {
                flow.value += it
            }.launchIn(lifecycleScope)
            service.connectionState.onEach {
                viewModel.connectionState.value = it
                if (it is ConnectionState.LoginRequired && isBound)
                    unbindService(this).also { isBound = false }
            }.launchIn(lifecycleScope)
            out.onEach {
                service.send(it)
            }.launchIn(lifecycleScope)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LL.e(name)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    private fun bind() {
        bindService(
            Intent(this, WebSocketService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }
}


