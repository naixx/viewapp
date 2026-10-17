package com.github.naixx.viewapp.network

import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.naixx.logger.LL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

// Firmware defaults (VIEW system/wifi.js apName/apPass).
private const val VIEW_AP_SSID = "TL+VIEW"
private const val VIEW_AP_PASSWORD = "timelapse+"
private const val MIN_RETRY_DELAY_MS = 5_000L
private const val MAX_RETRY_DELAY_MS = 120_000L

/**
 * Keeps the phone on the VIEW's own hotspot for as long as the calling coroutine runs.
 *
 * The hotspot has no internet, so Android leaves it for mobile data (the device logs
 * `ap recv deauth reason code(3)`) and does not auto-join it again after it restarts. A
 * [WifiNetworkSpecifier] request is a local-only connection that is not dropped for lacking internet;
 * it is re-requested after every loss, backing off while the hotspot keeps not showing up. The process
 * is bound to it because it is never the default network, so sockets to 10.0.0.1 would otherwise go
 * out over mobile data. Cancel to let go.
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun ConnectivityManager.holdViewAp() {
    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .setNetworkSpecifier(
            WifiNetworkSpecifier.Builder().setSsid(VIEW_AP_SSID).setWpa2Passphrase(VIEW_AP_PASSWORD).build()
        )
        .build()
    var retryDelay = MIN_RETRY_DELAY_MS
    while (true) {
        // Completes with whether we were connected: onLost follows onAvailable, onUnavailable never does.
        val gone = CompletableDeferred<Boolean>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                LL.i("$VIEW_AP_SSID available")
                bindProcessToNetwork(network)
            }

            override fun onLost(network: Network) {
                LL.w("$VIEW_AP_SSID lost")
                gone.complete(true)
            }

            override fun onUnavailable() {
                LL.w("$VIEW_AP_SSID not found or not approved")
                gone.complete(false)
            }
        }
        requestNetwork(request, callback)
        val wasConnected = try {
            gone.await()
        } finally {
            // After onUnavailable the system has already released the request and unregistering again
            // throws. Unregister before unbinding so a queued onAvailable can't re-bind a network we left.
            runCatching { unregisterNetworkCallback(callback) }
            bindProcessToNetwork(null)
        }
        retryDelay = if (wasConnected) MIN_RETRY_DELAY_MS else (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        delay(retryDelay)
    }
}
