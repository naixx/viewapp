package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import com.github.naixx.viewapp.ui.components.FrameHistogramView
import com.github.naixx.viewapp.ui.components.FrameProgressBar
import com.github.naixx.viewapp.ui.components.HistogramView
import com.github.naixx.viewapp.ui.components.LiveEditButton
import com.github.naixx.viewapp.ui.components.LiveSelectButton
import com.github.naixx.viewapp.ui.components.OnConnectedEffect
import com.github.naixx.viewapp.ui.components.Opt
import com.github.naixx.viewapp.ui.components.RampInfo
import com.github.naixx.viewapp.ui.components.TimelapsePlayer
import com.github.naixx.viewapp.ui.components.VButton
import com.github.naixx.viewapp.ui.components.VSmallButton
import com.github.naixx.viewapp.ui.components.cameraSettings
import com.github.naixx.viewapp.ui.components.sampleHistogram
import com.github.naixx.viewapp.ui.components.sampleProgram
import com.github.naixx.viewapp.ui.components.sampleProgramRaw
import com.github.naixx.viewapp.ui.components.status
import com.github.naixx.viewapp.utils.activityViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import com.github.naixx.viewapp.utils.formatted
import github.naixx.network.Battery
import github.naixx.network.CameraDetails
import github.naixx.network.CameraSettings
import github.naixx.network.Clip
import github.naixx.network.ConnectedMessage
import github.naixx.network.ConnectionState
import github.naixx.network.DynamicChange
import github.naixx.network.DynamicChangeEntry
import github.naixx.network.Get
import github.naixx.network.IntervalometerStatus
import github.naixx.network.OutMessage
import github.naixx.network.Program
import github.naixx.network.ProgramDraft
import github.naixx.network.SettingsMessage
import github.naixx.network.Shutter
import github.naixx.network.Status
import github.naixx.network.Stop
import github.naixx.network.UpdateProgram
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Live (dynamicChange) mode options — only auto/fixed are switchable mid-run.
private val liveRampModes = listOf(Opt("Auto Ramping", "auto"), Opt("Fixed", "fixed"))
private val liveIntervalModes = listOf(Opt("Auto Day/Night", "auto"), Opt("Fixed", "fixed"))

private fun DynamicChangeEntry.toRampInfo() = RampInfo(
    from = startVal ?: 0.0,
    to = endVal ?: 0.0,
    startFrame = startFrame ?: 0,
    endFrame = endFrame ?: 0,
)

class StatusScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = activityViewModel<MainViewModel>()
        val conn by viewModel.connectionState.collectAsState()
        val connected by viewModel.connectedMessage.collectAsState()
        val settings by viewModel.settingsMessage.collectAsState()
        val battery by viewModel.batteryMessage.collectAsState()

        val intervalStatus by viewModel.intervalometerStatus.collectAsState()
        val histogram by viewModel.histogram.collectAsState()
        val program by viewModel.program.collectAsState()
        val thumbnail by viewModel.thumbnail.collectAsState()

        val programVm = activityViewModel<ProgramViewModel>()
        val draft by programVm.draft.collectAsState()

        val bound = conn !is ConnectionState.Disconnected // a connection is wanted: the Connect / Disconnect button
        val subnetLabel by viewModel.subnetLabel.collectAsState()
        val notifications by viewModel.notifications.collectAsState()

        OnConnectedEffect(conn) {
            viewModel.send(Get("program"))
        }
        LaunchedEffect(program) { programVm.onDeviceProgram(program?.raw) }

        val send: (OutMessage) -> Unit = { viewModel.send(it) }

        val clip = remember(intervalStatus) {
            intervalStatus?.status?.tlName?.let { tlName ->
                Clip(
                    index = 0,
                    id = tlName.hashCode(),
                    name = tlName,
                    frames = intervalStatus?.status?.frames ?: 0,
                    imageBase64 = ""
                )
            }
        }
        val timelapseViewModel = clip?.let { tl ->
            // Key by timelapse name so a new capture (tl-415) gets a fresh VM instead of reusing the
            // previous one (tl-414): the ViewModelStore caches by key, and without one the clip change
            // would be ignored, leaving the old clip's directory wired up for reads/writes.
            assistedMetroViewModel<TimelapseViewModel, TimelapseViewModel.Factory>(key = tl.name) { create(tl) }
        }

        LaunchedEffect(clip) {
            clip?.let {
                timelapseViewModel?.checkExistingFrames(it.frames)
            }
        }

        // Frames of the running time-lapse are cached by DeviceState as they arrive and reach the view model
        // through DeviceState.frameWritten.

        val downloadedFrames by timelapseViewModel?.downloadedFrames?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
        val currentFrameIndex by timelapseViewModel?.currentFrameIndex?.collectAsState() ?: remember { mutableStateOf(0) }
        val isPlaying by timelapseViewModel?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
        val isDownloading by timelapseViewModel?.isDownloading?.collectAsState() ?: remember { mutableStateOf(false) }

        val frames = downloadedFrames
        val progress = remember(frames, clip) {
            if (clip != null && clip.frames > 0 && frames.isNotEmpty()) {
                // Clamp: live frames can outpace the stale clip.frames; without this progress goes
                // past 1f and PlayerControls renders no button at all.
                (frames.size.toFloat() / clip.frames).coerceAtMost(1f)
            } else if (frames.isNotEmpty()) {
                1f
            } else {
                0f
            }
        }

        // Nothing to fetch before the first frame, and until then the VIEW may not have the clip yet:
        // /clipinfo answers an empty 200 and the download failed right after every start.
        if (conn is ConnectionState.Connected && timelapseViewModel != null && clip.frames > 0)
            LaunchedEffect(Unit) {
                timelapseViewModel.downloadFrames(conn)
            }


        LaunchedEffect(isPlaying, currentFrameIndex, frames.size) {
            if (isPlaying && frames.isNotEmpty() && timelapseViewModel != null) {
                while (isPlaying) {
                    delay(33)
                    // Advance by position in the (possibly gappy) downloaded set, wrapping at the end.
                    val pos = frames.indexOf(currentFrameIndex)
                    val nextFrame = if (pos < 0 || pos >= frames.size - 1) frames.first() else frames[pos + 1]
                    timelapseViewModel.setCurrentFrame(nextFrame)
                }
            }
        }



        DisposableEffect(key1 = Unit) {
            onDispose {
                timelapseViewModel?.togglePlayback(false)
                if (progress > 0f && progress < 1f) {
                    timelapseViewModel?.stopDownload()
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    // The browser has nothing to start or stop: the page is the connection.
                    if (viewModel.controllable) ServiceControls(bound, conn, subnetLabel, { viewModel.connect() }, { viewModel.disconnect() })
                }
                NotificationBell(
                    notifications = notifications,
                    onRead = { viewModel.markNotificationsRead() },
                    onClear = { viewModel.clearNotifications() },
                )
            }

            if (intervalStatus?.status?.running == true) {
                if (clip != null && timelapseViewModel != null) {
                    // No download/resume controls while capturing: frames stream to disk live
                    // (addLiveFrame), so there is nothing to "resume" — and basing a button on the
                    // live progress fraction made it blink each time a thumbnail lagged the status
                    // frame count. Full frames are downloaded later from the Clips browser.
                    TimelapsePlayer(
                        frames = frames,
                        progress = progress,
                        currentFrameIndex = currentFrameIndex,
                        isPlaying = isPlaying,
                        isLive = true,
                        isDownloading = isDownloading,
                        clip = clip.name,
                        onPlayPause = { timelapseViewModel.togglePlayback(!isPlaying) },
                        onSeek = { timelapseViewModel.setCurrentFrame(it) },
                        onScrubStart = { timelapseViewModel.togglePlayback(false) },
                        onGoLive = { timelapseViewModel.goLive() }
                    )
                } else {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop
                    )
                }

                intervalStatus?.status?.let { status ->
                    Spacer(modifier = Modifier.height(4.dp))
                    FrameProgressBar(status, deviceNowMs = { viewModel.deviceClock.nowMs() })
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (timelapseViewModel != null) {
                    FrameHistogramView(currentFrameIndex) { timelapseViewModel.histogram(it) }
                } else {
                    HistogramView(histogram?.histogram)
                }

                TimeLapseStatus(bound, connected, intervalStatus?.status, program?.program, program?.raw, settings?.settings?.details, send)

                Spacer(modifier = Modifier.height(16.dp))
                StopButton(send)
            } else {
                // Idle: manual camera control (when a camera is connected) above the program editor
                // (mirrors the original CameraPage + IntervalometerPage setup view).
                if (connected?.connected == true) {
                    settings?.settings?.let { CameraControls(it, thumbnail, send) }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                }
                draft?.let { d ->
                    ProgramEditor(d, programVm, settings?.settings, send)
                }
            }
            BatteryInfo(connected, settings, battery, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(8.dp))

        }
    }

    @Composable
    private fun StopButton(send: (OutMessage) -> Unit) {
        var showStopConfirm by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VButton(
                "STOP",
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { showStopConfirm = true }
        }
        if (showStopConfirm) {
            AlertDialog(
                onDismissRequest = { showStopConfirm = false },
                title = { Text("Stop time-lapse?") },
                text = { Text("This stops the running time-lapse after the current frame completes.") },
                confirmButton = {
                    TextButton(onClick = { send(Stop()); showStopConfirm = false }) { Text("Stop") }
                },
                dismissButton = { TextButton(onClick = { showStopConfirm = false }) { Text("Cancel") } },
            )
        }
    }

    @Composable
    private fun TimeLapseStatus(
        bound: Boolean,
        connected: ConnectedMessage?,
        status: Status?,
        program: Program?,
        programRaw: JsonObject?,
        // The camera's value lists for the limits UI, from the settings message. The status carried them too until
        // VIEW firmware 2026-10-21 (15 of its 17 KB, sent on every frame); older firmware still fills them there.
        details: CameraDetails?,
        send: (OutMessage) -> Unit,
    ) {
        if (!bound) return

        Text(
            text = "Time-lapse " + status?.tlName,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Camera-connection line needs the ConnectedMessage for the model name. It's optional —
        // the device sends it once on camera connect, so it can be absent for a whole session.
        connected?.let {
            Text(
                text = cameraStatusLine(status?.message, it),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Live-edit controls depend only on the running status, NOT on the camera ConnectedMessage.
        // Gating them behind `connected` hid the whole running-edit UI whenever that message was
        // missing (which is exactly what was happening), so they live under `status` directly.
        status?.let { status ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = framesLine(status.frames),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (status.rampMode == "fixed") {
                    val total = status.frames + (status.framesRemaining ?: 0)
                    LiveEditButton(
                        text = "OF $total",
                        title = "Total Frames",
                        current = total.toDouble(),
                        rampable = false,
                    ) { v, _ -> send(DynamicChange("frames", JsonPrimitive(v), 0)) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            RampingSection(status, send)

            if (status.rampMode.equals("auto", ignoreCase = true) && programRaw != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LiveRampingOptions(programRaw, details ?: status.cameraSettings?.details, send)
            }

            Spacer(modifier = Modifier.height(8.dp))

            ExposureSection(status.cameraSettings)

            Spacer(modifier = Modifier.height(8.dp))

            IntervalSection(status, program, send)
        }
    }

    @Composable
    private fun RampingSection(status: Status, send: (OutMessage) -> Unit) {
        val rampEv = status.exposure.status.manualOffsetEv?.formatted() ?: "0"
        val dayRefEv = status.exposure.status.dayRefEv?.formatted(1) ?: "0"
        val nigRefEv = status.exposure.status.nightRefEv?.formatted(1) ?: "0"
        val isFixed = status.rampMode.equals("fixed", ignoreCase = true)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ramping:",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveSelectButton(
                    text = status.rampMode ?: "unknown",
                    title = "Exposure Mode",
                    options = liveRampModes,
                    current = status.rampMode,
                ) { send(DynamicChange("rampMode", JsonPrimitive(it), 0)) }

                val offsetParam = if (isFixed) "rampEv" else "manualOffsetEv"
                LiveEditButton(
                    text = "OFFSET $rampEv STOPS",
                    title = if (isFixed) "Manual Ramp" else "Exposure Offset",
                    current = if (isFixed) 0.0 else (status.exposure.status.manualOffsetEv ?: 0.0),
                    rampable = true,
                    active = status.dynamicChange[offsetParam]?.toRampInfo(),
                    currentFrame = status.frames,
                ) { v, f -> send(DynamicChange(offsetParam, JsonPrimitive(v), f)) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveEditButton(
                text = "DAY $dayRefEv",
                title = "Day Exposure Reference",
                current = status.exposure.status.dayRefEv ?: 0.0,
                rampable = true,
                active = status.dynamicChange["dayRefEv"]?.toRampInfo(),
                currentFrame = status.frames,
            ) { v, f -> send(DynamicChange("dayRefEv", JsonPrimitive(v), f)) }

            Slider(
                value = status.exposure.status.nightRatio?.toFloat() ?: 0.5f,
                onValueChange = { },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            LiveEditButton(
                text = "NIGHT $nigRefEv",
                title = "Night Exposure Reference",
                current = status.exposure.status.nightRefEv ?: 0.0,
                rampable = true,
                active = status.dynamicChange["nightRefEv"]?.toRampInfo(),
                currentFrame = status.frames,
            ) { v, f -> send(DynamicChange("nightRefEv", JsonPrimitive(v), f)) }
        }
    }

    /**
     * The ramping options of the running time-lapse: its exposure limits, and the options dialog whose edits go
     * to the VIEW with updateProgram. The core reads the limits and ramp parameters each frame.
     */
    @Composable
    private fun LiveRampingOptions(programRaw: JsonObject, details: CameraDetails?, send: (OutMessage) -> Unit) {
        // Edits show at once; the device's program replaces them when it answers Get("program").
        var draft by remember(programRaw) { mutableStateOf(ProgramDraft(programRaw)) }
        var open by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rampLimitsLine(draft, details),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            VSmallButton(rampingSummary(draft)) { open = true }
        }
        if (open) {
            RampingOptionsDialog(
                draft, details, live = true,
                onChange = { key, value ->
                    draft = draft.set(key, value)
                    send(UpdateProgram(JsonObject(mapOf(key to value))))
                    send(Get("program"))
                },
                onDismiss = { open = false },
            )
        }
    }

    @Composable
    private fun ExposureSection(settings: CameraSettings?) {
        val shutter = settings?.shutter ?: "unknown"
        val aperture = settings?.aperture ?: "UNKNOWN"
        val iso = settings?.iso ?: "unknown"

        Text(
            text = "Exposure: $shutter f/$aperture $iso ISO",
            style = MaterialTheme.typography.bodyMedium
        )
        driveModeLine(settings?.driveMode)?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    private fun IntervalSection(status: Status, program: Program?, send: (OutMessage) -> Unit) {
        val intervalSeconds = (status.intervalMs / 1000).formatted(1)
        if (program?.rampMode.equals("fixed", ignoreCase = true)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interval: ${intervalSeconds}s",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.weight(1f))

                LiveEditButton(
                    text = "EDIT",
                    title = "Interval",
                    current = status.intervalMs / 1000.0,
                    rampable = true,
                    active = status.dynamicChange["interval"]?.toRampInfo(),
                    currentFrame = status.frames,
                ) { v, f -> send(DynamicChange("interval", JsonPrimitive(v), f)) }

                LiveSelectButton(
                    text = "FIXED",
                    title = "Interval Mode",
                    options = liveIntervalModes,
                    current = "fixed",
                ) { send(DynamicChange("intervalMode", JsonPrimitive(it), 0)) }
            }
        } else if (program?.rampMode.equals("auto", ignoreCase = true)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Current Interval: ${intervalSeconds}s",
                    style = MaterialTheme.typography.bodyMedium
                )

                LiveSelectButton(
                    text = "AUTO",
                    title = "Interval Mode",
                    options = liveIntervalModes,
                    current = "auto",
                ) { send(DynamicChange("intervalMode", JsonPrimitive(it), 0)) }
            }
            program?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Day Interval: ${program.dayInterval.formatted(1)}s",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LiveEditButton(
                        text = "Edit",
                        title = "Day Interval",
                        current = program.dayInterval,
                        rampable = true,
                        active = status.dynamicChange["dayInterval"]?.toRampInfo(),
                        currentFrame = status.frames,
                    ) { v, f -> send(DynamicChange("dayInterval", JsonPrimitive(v), f)) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Night Interval: ${program.nightInterval.formatted(1)}s",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LiveEditButton(
                        text = "Edit",
                        title = "Night Interval",
                        current = program.nightInterval,
                        rampable = true,
                        active = status.dynamicChange["nightInterval"]?.toRampInfo(),
                        currentFrame = status.frames,
                    ) { v, f -> send(DynamicChange("nightInterval", JsonPrimitive(v), f)) }
                }
            }
        }
    }

    @Composable
    private fun ServiceControls(bound: Boolean, conn: ConnectionState, subnet: String?, onStart: () -> Unit, onStop: () -> Unit) {
        if (!bound) {
            VSmallButton(if (subnet != null) "Connect · $subnet" else "Connect") {
                onStart()
            }
        } else {
            val connectionString = when (val con = conn) {
                is ConnectionState.Connected -> "Disconnect from " + con.address.address
                is ConnectionState.Connecting -> "Stop connecting to VIEW"
                else -> "Stop"
            }

            VSmallButton(connectionString) {
                onStop()
            }
        }
    }

    @Composable
    private fun BatteryInfo(connected: ConnectedMessage?, settings: SettingsMessage?, battery: Battery?, modifier: Modifier = Modifier) {
        connected?.let {
            val text = listOfNotNull(
                it.model.takeIf { it.isNotEmpty() },
                settings?.settings?.battery?.let { b -> "${b.toInt()}%" },
                battery?.let { "VIEW battery " + it.percentage.toInt() + "%" }
            ).joinToString()

            Text(text, style = MaterialTheme.typography.bodySmall, modifier = modifier)
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun HistogramPreview() {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            HistogramView(sampleHistogram.histogram)
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun RampingSectionPreview() {
        val previewStatus = status.copy(
            frames = 57,
            message = "running",
            rampMode = "AUTO"
        )

        Surface {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                RampingSection(previewStatus, send = {})
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun LiveRampingOptionsPreview() {
        Surface {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                LiveRampingOptions(sampleProgramRaw, cameraSettings.details, send = {})
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun ExposureSectionPreview() {
        val settings = SettingsMessage(
            settings = cameraSettings.copy(
                shutter = "1/5",
                aperture = "UNKNOWN",
                iso = "500"
            ),
            type = "settings"
        )

        Surface {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                ExposureSection(settings.settings)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun IntervalSectionPreview() {
        val previewStatus = status.copy(
            frames = 57,
            intervalMs = 60000.0,
            message = "running"
        )

        Surface {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                IntervalSection(previewStatus, sampleProgram, send = {})
            }
        }
    }

    @Preview(showBackground = true, widthDp = 400, heightDp = 600)
    @Composable
    private fun FullStatusScreenPreview() {
        val connected = ConnectedMessage(
            connected = true,
            model = "Sony A6600",
            supports = null,
            ack = null,
            type = "camera"
        )

        val settings = SettingsMessage(
            settings = cameraSettings,
            type = "settings"
        )

        val battery = Battery(62.5f, false, "battery")

        val previewStatus = status.copy(
            frames = 57,
            framesRemaining = 243,
            intervalMs = 60000.0,
            message = "running",
            rampMode = "fixed",
            captureStartTime = 1000.0,
            cameraSettings = cameraSettings.copy(shutter = "8s", details = CameraDetails(shutter = Shutter("8s", ev = -9.0))),
            exposure = status.exposure.copy(
                status = status.exposure.status.copy(
                    manualOffsetEv = 0.0,
                    dayRefEv = -7.1,
                    nightRefEv = -8.4,
                    nightRatio = 0.7
                )
            )
        )

        val intervalStatus = IntervalometerStatus(
            status = previewStatus,
            ack = null,
            type = "intervalometerStatus"
        )

        Surface {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // 25 s into the 60 s interval: 8 s exposure and 2 s processing are done, waiting for the next frame.
                FrameProgressBar(previewStatus, deviceNowMs = { 1_025_000L })
                Spacer(modifier = Modifier.height(8.dp))
                HistogramView(sampleHistogram.histogram)
                Spacer(modifier = Modifier.height(8.dp))
                TimeLapseStatus(true, connected, intervalStatus.status, sampleProgram, sampleProgramRaw, cameraSettings.details, send = {})
                Spacer(modifier = Modifier.height(8.dp))
                ServiceControls(true, ConnectionState.Connecting, null, {}, {})
                Spacer(modifier = Modifier.height(8.dp))
                BatteryInfo(connected, settings, battery)
            }
        }
    }
}

