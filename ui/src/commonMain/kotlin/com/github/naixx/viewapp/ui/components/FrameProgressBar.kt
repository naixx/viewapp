package com.github.naixx.viewapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.naixx.network.CameraDetails
import github.naixx.network.Shutter
import github.naixx.network.Status
import kotlinx.coroutines.delay
import kotlin.math.pow

/** Parts of the interval, as fractions of it: the exposure, the download and processing after it, and the time since the capture started. */
data class FrameCycle(val exposure: Float, val processing: Float, val elapsed: Float)

/**
 * The frame bar from the VIEW's OLED (`oled.js` drawTimeLapseStatus): exposure, then download and processing
 * (`autoSettings.paddingTimeMs`, the measured buffer time + 0.5 s), then the wait for the next frame. The
 * marker is pinned at the end while the next frame is late.
 */
fun frameCycle(status: Status, deviceNowMs: Long): FrameCycle? {
    val start = status.captureStartTime ?: return null
    val interval = status.intervalMs / 1000
    if (!status.running || interval <= 0) return null
    val shutter = status.cameraSettings?.details?.shutter?.ev?.let(::shutterSeconds) ?: 0.0
    fun part(seconds: Double) = (seconds / interval).coerceIn(0.0, 1.0).toFloat()
    return FrameCycle(part(shutter), part(status.autoSettings.paddingTimeMs / 1000), part(deviceNowMs / 1000.0 - start))
}

/** Exposure time of a shutter EV as the VIEW reckons it (`lists.getSecondsFromEv`: 1/64 s at 0 EV, 0.1 s above). */
fun shutterSeconds(ev: Double): Double = if (ev > 0) 0.1 else 2.0.pow(-ev) / 64

private val ExposureColor = Color(0xFFE53935)
private val ProcessingColor = Color(0xFF3D5AFE)

@Composable
fun FrameProgressBar(status: Status, deviceNowMs: () -> Long, modifier: Modifier = Modifier) {
    val now by produceState(deviceNowMs()) {
        while (true) {
            value = deviceNowMs()
            delay(50)
        }
    }
    val cycle = frameCycle(status, now) ?: return
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val marker = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.fillMaxWidth().height(8.dp)) {
        val lineHeight = 2.dp.toPx()
        val top = (size.height - lineHeight) / 2
        val exposureEnd = size.width * cycle.exposure
        val processingEnd = (size.width * (cycle.exposure + cycle.processing)).coerceAtMost(size.width)
        drawRect(track, Offset(0f, top), Size(size.width, lineHeight))
        drawRect(ExposureColor, Offset(0f, top), Size(exposureEnd, lineHeight))
        drawRect(ProcessingColor, Offset(exposureEnd, top), Size(processingEnd - exposureEnd, lineHeight))
        val markerWidth = 1.dp.toPx()
        val x = (size.width * cycle.elapsed - markerWidth / 2).coerceIn(0f, size.width - markerWidth)
        drawRect(marker, Offset(x, 0f), Size(markerWidth, size.height))
    }
}

@Preview
@Composable
fun FrameProgressBarPreview() {
    // A night frame: 4 s exposure, 2 s download and processing, 20 s interval.
    val night = status.copy(
        intervalMs = 20_000.0,
        captureStartTime = 1000.0,
        cameraSettings = cameraSettings.copy(details = CameraDetails(shutter = Shutter("4s", ev = -8.0))),
    )
    MaterialTheme(darkColorScheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("exposing" to 2_000, "downloading" to 5_000, "waiting" to 12_000, "next frame late" to 25_000)
                    .forEach { (label, elapsedMs) ->
                        Text(label, style = MaterialTheme.typography.labelSmall)
                        FrameProgressBar(night, deviceNowMs = { 1_000_000L + elapsedMs })
                    }
            }
        }
    }
}
