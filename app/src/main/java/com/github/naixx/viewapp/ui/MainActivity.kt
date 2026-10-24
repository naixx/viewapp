package com.github.naixx.viewapp.ui

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.*
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import androidx.activity.viewModels
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.ui.components.LocalSuperNavigator
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.AppForeground
import com.github.naixx.viewapp.utils.ClipMapper
import com.github.naixx.viewapp.utils.ToastMessageHandler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import github.naixx.core.DeviceSession
import github.naixx.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Built by the graph through MetroAppComponentFactory (see AppGraph), hence the constructor. The view-model factory
 * serves `by viewModels` here and, as `LocalMetroViewModelFactory`, the screens' `activityViewModel()` / `screenViewModel()`.
 */
@ActivityKey
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
class MainActivity(
    private val storageProvider: StorageProvider,
    private val session: DeviceSession,
    private val api: ViewApi,
    private val viewModelFactory: MetroViewModelFactory,
) : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            LL.e("Notification permission granted")
        } else {
            LL.e("Notification permission denied")
        }
    }
    private val viewModel: MainViewModel by viewModels { viewModelFactory }

    /** Whether a connection is wanted (the Status tab's Connect / Disconnect button). */
    val isBound: StateFlow<Boolean> by lazy {
        session.state.map { it !is ConnectionState.Disconnected }.stateIn(lifecycleScope, SharingStarted.Eagerly, false)
    }

    @OptIn(ExperimentalVoyagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
        session.connect()
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
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                ViewAppTheme {
                    Surface(Modifier.fillMaxSize()) {

                        val conn by viewModel.connectionState.collectAsState()

                        Navigator(
                            MainScreen, disposeBehavior = NavigatorDisposeBehavior( //to preserve models
                                disposeNestedNavigators = false,
                                disposeSteps = true
                            )
                        ) { superNav ->
                            CompositionLocalProvider(LocalSuperNavigator provides superNav) {
                                CurrentScreen()
                            }
                        }
                        val c = conn
                        if (c is ConnectionState.LoginRequired) {
                            LoginDialog(
                                onLogin = { username, password ->
                                    apiLogin(username, password)
                                }
                            )
                        }
                    }
                    ToastMessageHandler()

                }
            }
        }
    }

    private fun apiLogin(username: String, password: String) {
        LL.e("Login attempt with $username")
        lifecycleScope.launch {
            try {
                // The api follows the connection: LoginRequired carries the host that asked.
                val auth = api.auth(mapOf("email" to username, "password" to password))
                storageProvider.session(auth.session)
                LL.e("Login successful, session: ${auth.session}")
                startService()
            } catch (e: Exception) {
                LL.e("Login failed: ${e.message}")
                //TODO show toast
            }
        }
    }

    fun startService() = session.connect()

    fun stopService() = session.disconnect()

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

    override fun onStart() {
        super.onStart()
        AppForeground.isForeground = true
    }

    override fun onStop() {
        super.onStop()
        AppForeground.isForeground = false
    }
}


