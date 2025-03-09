package com.github.naixx.viewapp

import android.content.*
import android.net.nsd.*
import android.net.wifi.WifiManager
import android.os.*
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import github.naixx.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import org.koin.android.ext.android.*
import org.koin.core.parameter.parametersOf
import java.io.File
import java.net.InetAddress

fun getConnectedDevices(): List<String> {
    val devices = mutableListOf<String>()
    try {
        val arpFile = File("/proc/net/arp")
        if (!arpFile.exists()) return devices

        arpFile.bufferedReader().useLines { lines ->
            // Skip header line
            lines.drop(1).forEach { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val ip = parts[0]
                    val mac = parts[3]
                    // Filter out incomplete entries
                    if (mac != "00:00:00:00:00:00") {
                        devices.add(ip)
                    }
                }
            }
        }
    } catch (e: IOException) {
        LL.e(e)
    }
    return devices
}

class NsdHelper(
    private val context: Context,
    private val serviceType: String,
    private val deviceName: String
) {

    private lateinit var nsdManager: NsdManager
    private var resolvedService: NsdServiceInfo? = null

    fun initialize() {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        discoverServices()
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            // Handle failure
            LL.e("")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            LL.e("")
            // Handle failure
        }

        override fun onDiscoveryStarted(serviceType: String) {
            LL.e("")
            // Discovery started
        }

        override fun onDiscoveryStopped(serviceType: String) {
            LL.e("")
            // Discovery stopped
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            LL.e("")
            // Found a service!
            if (serviceInfo.serviceType == serviceType) {
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            LL.e("")
            // Service went away
        }
    }
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Handle failure
            LL.e("-->")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            LL.e("-->" + serviceInfo.serviceName)
            if (serviceInfo.serviceName.contains(deviceName, ignoreCase = true)) {
                resolvedService = serviceInfo
                // Found our device! Now you can connect using:
                // serviceInfo.host
                // serviceInfo.port
            }
        }
    }

    private fun discoverServices() {
        nsdManager.discoverServices(
            serviceType,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun tearDown() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
}

fun findDeviceByName(context: Context, deviceName: String): InetAddress? {
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo
    val ipAddress = wifiInfo.ipAddress
    val ip = Formatter.formatIpAddress(ipAddress);
    // Convert IP address to human-readable format (192.168.1.x)
    val networkBase = String.format(
        "%d.%d.%d",
        (ipAddress and 0xFF),           // Correct: 192 from -81811264
        (ipAddress shr 8 and 0xFF),     // Correct: 168
        (ipAddress shr 16 and 0xFF)     // Correct: 31
    )
    // Scan the local network
    for (i in 1..254) {
        val host = "$networkBase.$i"
        try {
            val address = InetAddress.getByName(host)
            if (address.isReachable(5)) { // Timeout in milliseconds
                val hostName = address.hostName
                LL.e(address.hostName + " $host")
                //  if (hostName.contains(deviceName, ignoreCase = true)) {
                //    return address
                // }
            }
        } catch (e: Exception) {
            // Handle exception or continue
            LL.e(e)
        }
    }
    return null
}

class MainActivity : ComponentActivity() {

    val flow = MutableStateFlow(emptyList<String>())
    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
        val intent = Intent(this, WebSocketService::class.java)
        startForegroundService(intent)
        bind()
        setContent {
            ViewAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by flow.collectAsState()
                    val conn by connectionState.collectAsState()

                    MainScreen(conn, state, modifier = Modifier.padding(innerPadding)) {
                        startService()
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
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    LL.e("Notification permission already granted")
                }

                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
                connectionState.value = it
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


