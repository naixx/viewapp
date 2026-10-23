package com.github.naixx.viewweb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import github.naixx.network.ConnectionState
import github.naixx.network.Get
import github.naixx.network.runDateMillis
import kotlinx.browser.window
import kotlin.math.roundToInt

/** Smoke screen for the shared protocol stack in the browser; the real screens come with the next steps. */
@Composable
fun App(connection: Connection) {
    val state by connection.state.collectAsState()
    val battery by connection.battery.collectAsState()
    val camera by connection.camera.collectAsState()
    val received by connection.received.collectAsState()
    val lastType by connection.lastType.collectAsState()
    // Exercises kotlinx-datetime's system time zone on wasm; shows the failure instead of crashing the page.
    val runDate = remember { runCatching { runDateMillis().toString() }.getOrElse { "error: $it" } }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("VIEW web", style = MaterialTheme.typography.headlineSmall)
                Text("Device: ${window.location.host}")
                Text(
                    "Connection: " + when (val s = state) {
                        ConnectionState.Disconnected -> "disconnected"
                        ConnectionState.Connecting -> "connecting…"
                        is ConnectionState.Connected -> "connected, ${s.address.address}"
                        is ConnectionState.LoginRequired -> "login required"
                    }
                )
                Text("Camera: " + (camera?.let { if (it.connected) it.model else "not connected" } ?: "—"))
                Text(
                    "Battery: " + (battery?.let {
                        "${it.percentage.roundToInt()}%" + if (it.charging) ", charging" else ""
                    } ?: "—")
                )
                Text("Messages: $received, last: ${lastType ?: "—"}")
                Button(onClick = { connection.send(Get("battery")) }) { Text("Refresh battery") }
                Text("runDateMillis(): $runDate", style = MaterialTheme.typography.bodySmall)
                Text(window.navigator.userAgent, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
