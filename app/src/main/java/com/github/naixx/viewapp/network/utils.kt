package com.github.naixx.viewapp.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import com.github.naixx.logger.LL
import github.naixx.network.buildSubnetUrls
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Builds the list of local hosts to probe when looking for the VIEW device.
 *
 * It derives the `/24` subnet of every active IPv4 interface (Wi-Fi, USB/AP tether, …) and
 * expands each to `.1`–`.254`, so the scan follows whatever network the phone is actually on
 * instead of a single hard-coded subnet. The access point address is always included.
 *
 * Note: on an emulator the only visible interface is the NAT'd `10.0.2.x`, so the host's real
 * LAN (e.g. `192.168.x.y`) cannot be discovered this way — that path relies on
 * `github.naixx.network.WIFI_URL` and the last-known-good addresses instead.
 */
fun generateLocalServer(c: Context): List<String> {
    val addresses = (deviceIpv4Addresses() + listOfNotNull(legacyWifiIp(c))).distinct()
    return buildSubnetUrls(addresses)
}

/**
 * A short `"192.168.4.x"`-style label of the Wi-Fi subnet the phone is currently on, for
 * display on the connect button. Returns null when no usable IPv4 address is found (e.g. offline).
 * Uses only the IP address (no location permission needed, unlike the Wi-Fi SSID).
 */
fun currentWifiSubnetLabel(c: Context): String? {
    val ip = legacyWifiIp(c) ?: deviceIpv4Addresses().firstOrNull()
    return ip?.substringBeforeLast('.')
        ?.takeIf { base -> base.count { it == '.' } == 2 }
        ?.let { "$it.x" }
}

/** Every non-loopback, non-link-local IPv4 address currently bound to an up interface. */
private fun deviceIpv4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().asSequence()
        .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
        .flatMap { it.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
        .mapNotNull { it.hostAddress }
        .toList()
}.getOrElse {
    LL.e(it)
    emptyList()
}

/** Legacy fallback: the Wi-Fi IP from the (deprecated) [WifiManager], or null if unavailable. */
@Suppress("DEPRECATION")
@SuppressLint("DefaultLocale")
private fun legacyWifiIp(c: Context): String? = runCatching {
    val wifiManager = c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ip = wifiManager.connectionInfo.ipAddress
    if (ip == 0) null
    else String.format(
        "%d.%d.%d.%d",
        ip and 0xFF,
        ip shr 8 and 0xFF,
        ip shr 16 and 0xFF,
        ip shr 24 and 0xFF,
    )
}.getOrNull()
