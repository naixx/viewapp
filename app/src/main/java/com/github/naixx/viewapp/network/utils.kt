package com.github.naixx.viewapp.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import github.naixx.network.ACCESS_POINT_URL


@SuppressLint("DefaultLocale")
fun generateLocalServer(c: Context): List<String> {
    val wifiManager = c.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo
    val ipAddress = wifiInfo.ipAddress
    // Convert IP address to human-readable format (192.168.1.x)
    val base = String.format(
        "http://%d.%d.%d.",
        (ipAddress and 0xFF),           // Correct: 192 from -81811264
        (ipAddress shr 8 and 0xFF),     // Correct: 168
        (ipAddress shr 16 and 0xFF)     // Correct: 31
    )
    return listOf(ACCESS_POINT_URL) + (1..254).map { "$base$it/" }
}
