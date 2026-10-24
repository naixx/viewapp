package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.naixx.viewapp.ui.components.Opt
import com.github.naixx.viewapp.ui.components.SelectDialog
import com.github.naixx.viewapp.ui.components.VButton
import com.github.naixx.viewapp.ui.components.VSmallButton
import com.github.naixx.viewapp.ui.components.cameraSettings
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.EventBus
import github.naixx.network.CameraSettings
import github.naixx.network.Capture
import github.naixx.network.CaptureTest
import github.naixx.network.Get
import github.naixx.network.ManualFocus
import github.naixx.network.OutMessage
import github.naixx.network.Preview as PreviewCmd
import github.naixx.network.PreviewStop
import github.naixx.network.SetEvDown
import github.naixx.network.SetEvUp
import github.naixx.network.SetSetting
import github.naixx.network.Thumbnail

/**
 * Compact manual camera control for the idle Status screen (shown only while connected to a camera):
 * the exposure triangle (shutter/aperture/ISO pickers) on one row, EV nudge + manual focus on the next,
 * then Photo / Test / Live view. A returned test-shot or live-view frame renders above.
 *
 * The device lists the full 1/3-stop range and tags codes it can't name as "UNKNOWN (<code>)"
 * (interpolated values that aren't real settings) — those are filtered out; AUTO has no ev but is kept.
 * The firmware does not auto-reply to `set`, so each change is followed by `Get("settings")`.
 *
 * Background (capture vs capture-test, full-range exposure lists, the live-view poll, imageType quirk):
 * docs/API.md "Manual camera control".
 */
@Composable
fun CameraControls(
    settings: CameraSettings,
    thumbnail: Thumbnail?,
    send: (OutMessage) -> Unit,
) {
    var liveViewOn by remember { mutableStateOf(false) }

    LaunchedEffect(liveViewOn) { if (liveViewOn) send(PreviewCmd()) }
    // Client-driven poll: re-request the next frame on every incoming thumbnail while live view is on.
    LaunchedEffect(thumbnail) { if (liveViewOn && thumbnail != null) send(PreviewCmd()) }
    DisposableEffect(Unit) { onDispose { if (liveViewOn) send(PreviewStop()) } }

    val shutterOpts = settings.details?.shutter?.list.orEmpty()
        .filterNot { it.name.startsWith("UNKNOWN") }.map { it.name to it.cameraName }
    val apertureOpts = settings.details?.aperture?.list.orEmpty()
        .filterNot { it.name.startsWith("UNKNOWN") }.map { it.name to it.cameraName }
    val isoOpts = settings.details?.iso?.list.orEmpty()
        .filterNot { it.name.startsWith("UNKNOWN") }.map { it.name to (it.cameraName ?: it.name) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Latest test-shot / live-view frame (CameraControls renders only when idle, so any frame here
        // is a manual capture/preview, not a running-timelapse frame).
//        if (thumbnail != null && thumbnail.imageBase64.isNotBlank()) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f),
                contentScale = ContentScale.Fit,
            )
//                }

        // Exposure triangle on one row.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposureChip("Shutter", "shutter", settings.shutter ?: "—", settings.shutter, shutterOpts, Modifier.weight(1f), send)
            ExposureChip("Aperture", "aperture", "f/${settings.aperture ?: "—"}", settings.aperture, apertureOpts, Modifier.weight(1f), send)
            ExposureChip("ISO", "iso", "ISO ${settings.iso ?: "—"}", settings.iso, isoOpts, Modifier.weight(1f), send)
        }
        driveModeLine(settings.driveMode)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        // EV nudge + manual focus on one row.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            VSmallButton("−") { send(SetEvDown()) }
            Text("EV", style = MaterialTheme.typography.labelMedium)
            VSmallButton("+") { send(SetEvUp()) }
            Spacer(Modifier.weight(1f))
            Text("Focus", style = MaterialTheme.typography.labelMedium)
            VSmallButton("◄◄") { send(ManualFocus("manual", -2, 1)) }
            VSmallButton("◄") { send(ManualFocus("manual", -1, 1)) }
            VSmallButton("►") { send(ManualFocus("manual", 1, 1)) }
            VSmallButton("►►") { send(ManualFocus("manual", 2, 1)) }
        }

        // Actions.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VSmallButton("Test(no sd)", modifier = Modifier.align(Alignment.CenterStart)) { send(CaptureTest()) }
            VButton("Photo") {
                send(Capture())
                EventBus.info("Capturing…")
            }
            VSmallButton(if (liveViewOn) "Stop" else "Live", modifier = Modifier.align(Alignment.CenterEnd)) {
                liveViewOn = !liveViewOn
                if (!liveViewOn) send(PreviewStop())
            }
        }
    }
}

/**
 * One compact exposure pill: shows [display], opens a single-choice picker built from [options]
 * (displayName → cameraName pairs). Selection sends `set` with the cameraName + a `Get("settings")`
 * refresh. Disabled (no-op) when the camera reports no options for this setting (e.g. wrong mode).
 */
@Composable
private fun ExposureChip(
    label: String,
    key: String,
    display: String,
    current: String?,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    send: (OutMessage) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    VSmallButton(display, modifier = modifier) { if (options.isNotEmpty()) open = true }
    if (open) {
        SelectDialog(
            title = label,
            options = options.map { Opt(it.first, it.first) },
            selected = current,
            onSelect = { name ->
                val cameraName = options.firstOrNull { it.first == name }?.second ?: name
                send(SetSetting(key, cameraName))
                send(Get("settings"))
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun CameraControlsPreview() {
    ViewAppTheme {
        Surface {
            Column(Modifier.padding(16.dp)) {
                CameraControls(cameraSettings, thumbnail = null, send = {})
            }
        }
    }
}
