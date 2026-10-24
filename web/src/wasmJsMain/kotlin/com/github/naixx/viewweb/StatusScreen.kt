package com.github.naixx.viewweb

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import github.naixx.core.DeviceState
import github.naixx.network.*
import kotlinx.browser.window
import org.jetbrains.skia.Image as SkiaImage
import kotlin.io.encoding.Base64
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Status page over the shared [DeviceState]: what the Android Status tab shows, minus the live ramping editors
 * and the program editor — a stand-in until the shared :ui screens land. Running: the live thumbnail, frames,
 * camera, exposure, interval, histogram and STOP; idle: the camera, the stored program and START.
 */
@Composable
fun StatusScreen(device: DeviceState) {
    val state by device.connection.collectAsState()
    val camera by device.camera.collectAsState()
    val settings by device.settings.collectAsState()
    val battery by device.battery.collectAsState()
    val intervalometer by device.intervalometerStatus.collectAsState()
    val thumbnail by device.thumbnail.collectAsState()
    val histogram by device.histogram.collectAsState()
    val program by device.program.collectAsState()
    val notifications by device.notifications.collectAsState()
    val status = intervalometer?.status

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConnectionLine(state)
        // Capture errors stay as notifications until read; no toasts here until the shared UI brings them.
        notifications.lastOrNull { !it.read }?.let { ErrorBanner(it.message, onDismiss = device::markNotificationsRead) }

        if (status?.running == true) {
            RunningSection(status, camera, thumbnail, histogram, send = device::send)
        } else {
            IdleSection(state, camera, settings, program, send = device::send)
        }

        BatteryLine(camera, settings, battery)
    }
}

@Composable
private fun ConnectionLine(state: ConnectionState) {
    val text = when (state) {
        ConnectionState.Disconnected -> "Disconnected"
        ConnectionState.Connecting -> "Connecting to ${window.location.host}…"
        is ConnectionState.Connected -> "Connected to ${window.location.host}"
        is ConnectionState.LoginRequired -> "Login required"
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun RunningSection(
    status: Status,
    camera: ConnectedMessage?,
    thumbnail: Thumbnail?,
    histogram: Histogram?,
    send: (OutMessage) -> Unit,
) {
    ThumbnailImage(thumbnail)
    HistogramView(histogram?.histogram)

    Text("Time-lapse ${status.tlName ?: ""}", style = MaterialTheme.typography.titleMedium)
    camera?.let { Text(cameraStatusLine(status.message, it)) }
    Text(framesLine(status.frames) + (status.framesRemaining?.let { " of ${status.frames + it}" } ?: ""))
    status.cameraSettings?.let { Text("Exposure: ${it.shutter ?: "unknown"} f/${it.aperture ?: "UNKNOWN"} ${it.iso ?: "unknown"} ISO") }
    Text("Interval: ${(status.intervalMs / 1000).fmt1()}s" + (status.rampMode?.let { " · $it ramping" } ?: ""))
    status.rampEv?.let { Text("Ramp: ${it.fmt1()} EV") }

    var confirmStop by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
            onClick = { confirmStop = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("STOP") }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop time-lapse?") },
            text = { Text("This stops the running time-lapse after the current frame completes.") },
            confirmButton = { TextButton(onClick = { send(Stop()); confirmStop = false }) { Text("Stop") } },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun IdleSection(
    state: ConnectionState,
    camera: ConnectedMessage?,
    settings: SettingsMessage?,
    program: TimelapseProgram?,
    send: (OutMessage) -> Unit,
) {
    Text(
        when {
            camera == null -> "Camera: —"
            camera.connected -> "Camera: ${camera.model.ifEmpty { "unknown" }}"
            else -> "No camera connected"
        },
        style = MaterialTheme.typography.titleMedium,
    )
    settings?.settings?.let { Text("Exposure: ${it.shutter ?: "unknown"} f/${it.aperture ?: "UNKNOWN"} ${it.iso ?: "unknown"} ISO") }

    HorizontalDivider()

    val draft = program?.let { ProgramDraft(it.raw) }
    if (draft == null) {
        Text("Program: —")
    } else {
        Text("Program", style = MaterialTheme.typography.titleMedium)
        Text(
            when (draft.intervalMode) {
                "fixed" -> "Interval: ${draft.interval}s"
                else -> "Interval: day ${draft.dayInterval.fmt1()}s, night ${draft.nightInterval.fmt1()}s (${draft.intervalMode})"
            }
        )
        Text("Ramping: ${draft.rampMode}" + if (draft.rampMode == "fixed") ", ${draft.frames} frames" else "")
        Text("Destination: ${draft.destination}")
    }

    var confirmStart by remember { mutableStateOf(false) }
    val canStart = state is ConnectionState.Connected && camera?.connected == true && draft != null
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(onClick = { confirmStart = true }, enabled = canStart) { Text("START") }
    }
    if (confirmStart && draft != null) {
        AlertDialog(
            onDismissRequest = { confirmStart = false },
            title = { Text("Start time-lapse?") },
            text = { Text("Starts the program stored on the VIEW with its current settings.") },
            confirmButton = {
                TextButton(onClick = {
                    // Always carry the start delay: a program without one never starts (ProgramViewModel.runMessage).
                    send(Run(draft.set("delay", draft.delay).json, runDateMillis()))
                    confirmStart = false
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { confirmStart = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BatteryLine(camera: ConnectedMessage?, settings: SettingsMessage?, battery: Battery?) {
    val text = listOfNotNull(
        camera?.model?.takeIf { it.isNotEmpty() },
        settings?.settings?.battery?.let { "${it.toInt()}%" },
        battery?.let { "VIEW battery ${it.percentage.toInt()}%" + if (it.charging) " (charging)" else "" },
    ).joinToString()
    if (text.isNotEmpty()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The latest frame the device pushed (a running time-lapse's thumbnail, or a live-view / test-shot preview). */
@Composable
private fun ThumbnailImage(thumbnail: Thumbnail?) {
    val bitmap: ImageBitmap? = remember(thumbnail?.imageBase64) {
        thumbnail?.imageBase64?.let { decodeJpeg(it) }
    }
    if (bitmap != null) {
        Image(
            bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
            Text("Waiting for the first frame…", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun decodeJpeg(base64: String): ImageBitmap? = runCatching {
    val bytes = Base64.Default.decode(base64.substringAfter("base64,", base64))
    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()

@Composable
private fun HistogramView(values: List<Int>?) {
    val bars = values?.takeIf { it.isNotEmpty() } ?: return
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val barWidth = size.width / bars.size
        val max = (bars.maxOrNull() ?: 1).coerceAtLeast(1)
        bars.forEachIndexed { index, value ->
            val h = value.toFloat() / max * size.height
            drawRect(color, topLeft = Offset(index * barWidth, size.height - h), size = Size(barWidth, h))
        }
    }
}

// The same lines as the Android Status tab (StatusScreen.kt); shared code comes with the :ui module later.
private fun cameraStatusLine(statusMessage: String?, camera: ConnectedMessage): String {
    val s = statusMessage ?: "unknown"
    return if (camera.connected) "$s | ${camera.model.ifEmpty { "unknown" }} connected" else "$s | camera disconnected"
}

private fun framesLine(frames: Int): String = "$frames frames (${(frames / 30.0).fmt1()}s @30fps)"

/** One decimal, without String.format (not on wasm). */
private fun Double.fmt1(): String {
    val tenths = (this * 10).roundToInt()
    return "${if (tenths < 0 && tenths > -10) "-" else ""}${tenths / 10}.${abs(tenths % 10)}"
}
