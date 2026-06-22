package com.github.naixx.viewapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import com.github.naixx.viewapp.ui.components.*
import com.github.naixx.viewapp.utils.*
import github.naixx.network.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonPrimitive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

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

        val c = LocalContext.current as MainActivity
        val bound by c.isBound.collectAsState()
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
        val timelapseViewModel = clip?.let {
            koinViewModel<TimelapseViewModel>(parameters = { parametersOf(it) })
        }
        val context = LocalContext.current

        LaunchedEffect(clip) {
            clip?.let {
                timelapseViewModel?.checkExistingFrames(context, it.frames)
            }
        }

        // Frames arrive live as Thumbnail messages while running — cache + show them immediately.
        LaunchedEffect(thumbnail) {
            val t = thumbnail ?: return@LaunchedEffect
            val tvm = timelapseViewModel
            if (intervalStatus?.status?.running == true && tvm != null && clip != null &&
                t.tlName.equals(clip.name, ignoreCase = true)
            ) {
                tvm.addLiveFrame(context, t.frameIndex, t.imageBase64)
            }
        }

        val downloadedFrames by timelapseViewModel?.downloadedFrames?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
        val currentFrameIndex by timelapseViewModel?.currentFrameIndex?.collectAsState() ?: remember { mutableStateOf(0) }
        val isPlaying by timelapseViewModel?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

        val frames = downloadedFrames
        val progress = remember(frames, clip) {
            if (clip != null && clip.frames > 0 && frames.isNotEmpty()) {
                frames.size.toFloat() / clip.frames
            } else if (frames.isNotEmpty()) {
                1f
            } else {
                0f
            }
        }


        LaunchedEffect(isPlaying, currentFrameIndex, frames.size) {
            if (isPlaying && frames.isNotEmpty() && timelapseViewModel != null) {
                while (isPlaying) {
                    delay(33)
                    val nextFrame = if (currentFrameIndex >= frames.size) 1 else currentFrameIndex + 1
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
                    ServiceControls(bound, conn, { c.startService() }, { c.stopService() })
                }
                NotificationBell(
                    notifications = notifications,
                    onRead = { viewModel.markNotificationsRead() },
                    onClear = { viewModel.clearNotifications() },
                )
            }

            if (intervalStatus?.status?.running == true) {
                if (clip != null && timelapseViewModel != null) {
                    TimelapsePlayer(
                        frames = frames,
                        progress = progress,
                        currentFrameIndex = currentFrameIndex,
                        context = context,
                        getFrameFile = { index -> timelapseViewModel.getFrameFile(context, index) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (frames.isNotEmpty()) {

                        PlayerSeekBar(
                            currentFrameIndex = currentFrameIndex,
                            framesCount = frames.size,
                            onFrameSelected = { timelapseViewModel.setCurrentFrame(it) }
                        )
                    }

                    PlayerControls(
                        progress = progress,
                        frames = frames,
                        isPlaying = isPlaying,
                        isDownloading = timelapseViewModel.isDownloading(),
                        onPlayPause = { timelapseViewModel.togglePlayback(!isPlaying) },
                        onReset = { timelapseViewModel.setCurrentFrame(1) },
                        onDownload = { timelapseViewModel.downloadFrames(context, conn) },
                        onCancelDownload = { timelapseViewModel.stopDownload() }
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

                Spacer(modifier = Modifier.height(8.dp))
                HistogramView(histogram)

                TimeLapseStatus(bound, connected, intervalStatus?.status, program?.program, send)

                Spacer(modifier = Modifier.height(16.dp))
                StopButton(send)
            } else {
                // Idle: show the program editor (mirrors the original IntervalometerPage setup view).
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
    private fun HistogramView(histogram: Histogram?) {
        histogram?.let {
            val color = MaterialTheme.colorScheme.onSurface
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val barWidth = size.width / it.histogram.size
                val maxValue = it.histogram.maxOrNull() ?: 1

                it.histogram.forEachIndexed { index, value ->
                    val height = (value.toFloat() / maxValue) * size.height
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(index * barWidth, size.height - height),
                        size = androidx.compose.ui.geometry.Size(barWidth, height)
                    )
                }
            }
        }
    }

    @Composable
    private fun TimeLapseStatus(
        bound: Boolean,
        connected: ConnectedMessage?,
        status: Status?,
        program: Program?,
        send: (OutMessage) -> Unit,
    ) {
        if (!bound) return

        Text(
            text = "Time-lapse " + status?.tlName,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        connected?.let {
            val s = status?.message ?: "unknown"
            val model = it.model
            Text(
                text = "$s | ${model.ifEmpty { "unknown" }} connected",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            status?.let { status ->
                val frames = status.frames
                val interval = status.intervalMs / 1000
                val duration = (frames * interval) / 30f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$frames frames (${String.format(Locale.US, "%.1f", duration)}s @30fps)",
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

                Spacer(modifier = Modifier.height(8.dp))

                ExposureSection(status.cameraSettings)

                Spacer(modifier = Modifier.height(8.dp))

                IntervalSection(status, program, send)
            }
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

    @Composable
    private fun ExposureSection(settings: CameraSettings?) {
        val shutter = settings?.shutter ?: "unknown"
        val aperture = settings?.aperture ?: "UNKNOWN"
        val iso = settings?.iso ?: "unknown"

        Text(
            text = "Exposure: $shutter f/$aperture $iso ISO",
            style = MaterialTheme.typography.bodyMedium
        )
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
    private fun ServiceControls(bound: Boolean, conn: ConnectionState, onStart: () -> Unit, onStop: () -> Unit) {
        if (!bound) {
            VSmallButton("Connect") {
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
                settings?.let { "${it.settings.battery?.toInt()}%" },
                battery?.let { "VIEW battery " + it.percentage.toInt() + "%" }
            ).joinToString()

            Text(text, style = MaterialTheme.typography.bodySmall, modifier = modifier)
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun HistogramPreview() {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            HistogramView(sampleHistogram)
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
                HistogramView(sampleHistogram)
                Spacer(modifier = Modifier.height(8.dp))
                TimeLapseStatus(true, connected, intervalStatus.status, sampleProgram, send = {})
                Spacer(modifier = Modifier.height(8.dp))
                ServiceControls(true, ConnectionState.Connecting, {}, {})
                Spacer(modifier = Modifier.height(8.dp))
                BatteryInfo(connected, settings, battery)
            }
        }
    }
}

