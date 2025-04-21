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
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

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

        val c = LocalContext.current as MainActivity
        val bound by c.isBound.collectAsState()

        OnConnectedEffect(conn) {
            viewModel.send(Get("program"))
        }

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
            ServiceControls(bound, conn, { c.startService() }, { c.stopService() })

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

                TimeLapseStatus(bound, connected, intervalStatus?.status, program?.program)
            }
            BatteryInfo(connected, settings, battery, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(8.dp))

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
        program: Program?
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
                    if (status.rampMode == "fixed")
                        VSmallButton(
                            text = "OF ${status.frames + (status.framesRemaining ?: 0)}"
                        )
                }

                Spacer(modifier = Modifier.height(8.dp))

                RampingSection(status)

                Spacer(modifier = Modifier.height(8.dp))

                ExposureSection(status.cameraSettings)

                Spacer(modifier = Modifier.height(8.dp))

                IntervalSection(status, program)
            }
        }
    }

    @Composable
    private fun RampingSection(status: Status) {
        val rampEv = status.exposure.status.manualOffsetEv?.formatted() ?: "0"
        val dayRefEv = status.exposure.status.dayRefEv?.formatted(1) ?: "0"
        val nigRefEv = status.exposure.status.nightRefEv?.formatted(1) ?: "0"

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
                VSmallButton(
                    text = status.rampMode ?: "unknown"
                )

                VSmallButton(
                    text = "OFFSET $rampEv STOPS"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VSmallButton(
                text = "DAY $dayRefEv"
            )

            Slider(
                value = status.exposure.status.nightRatio?.toFloat() ?: 0.5f,
                onValueChange = { },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            VSmallButton(
                text = "NIGHT $nigRefEv"
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
    }

    @Composable
    private fun IntervalSection(status: Status, program: Program?) {
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

                VSmallButton(
                    text = "EDIT"
                )

                VSmallButton(
                    text = "FIXED"
                )
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

                VSmallButton(
                    text = "AUTO"
                )
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

                    VSmallButton(
                        text = "Edit"
                    )
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

                    VSmallButton(
                        text = "Edit"
                    )
                }
            }
        }
    }

    @Composable
    private fun ServiceControls(bound: Boolean, conn: ConnectionState, onStart: () -> Unit, onStop: () -> Unit) {
        if (!bound) {
            VButton("Connect") {
                onStart()
            }
        } else {
            val connectionString = when (val con = conn) {
                is ConnectionState.Connected -> "Disconnect from " + con.address.address
                is ConnectionState.Connecting -> "Stop connecting to VIEW"
                else -> "Stop"
            }

            VButton(connectionString) {
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
                RampingSection(previewStatus)
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
                IntervalSection(previewStatus, sampleProgram)
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
                TimeLapseStatus(true, connected, intervalStatus.status, sampleProgram)
                Spacer(modifier = Modifier.height(8.dp))
                ServiceControls(true, ConnectionState.Connecting, {}, {})
                Spacer(modifier = Modifier.height(8.dp))
                BatteryInfo(connected, settings, battery)
            }
        }
    }
}
