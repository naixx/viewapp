package com.github.naixx.viewapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.naixx.viewapp.encoding.EncodingResult
import com.github.naixx.viewapp.ui.components.AppBarWithBack
import com.github.naixx.viewapp.ui.components.OnConnectedEffect
import com.github.naixx.viewapp.ui.components.TimelapsePlayer
import com.github.naixx.viewapp.ui.components.VButton
import com.github.naixx.viewapp.ui.components.sampleClipInfo
import com.github.naixx.viewapp.utils.EventBus
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.Clip
import github.naixx.network.TimelapseClipInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClipInfoScreen(val clip: Clip) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val mainViewModel = activityViewModel<MainViewModel>()
        val timelapseViewModel = koinViewModel<TimelapseViewModel>(key = clip.name, parameters = { parametersOf(clip) })
        val conn by mainViewModel.connectionState.collectAsState()
        val storedClipInfo by timelapseViewModel.clipInfo.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val downloadedFrames by timelapseViewModel.downloadedFrames.collectAsState()
        val currentFrameIndex by timelapseViewModel.currentFrameIndex.collectAsState()
        val isPlaying by timelapseViewModel.isPlaying.collectAsState()
        val isDownloading by timelapseViewModel.isDownloading.collectAsState()
        val exportProgress by timelapseViewModel.exportProgress.collectAsState()
        val intervalStatus by mainViewModel.intervalometerStatus.collectAsState()

        // "go live" only has meaning while this very clip is the time-lapse currently being captured.
        val isLive = intervalStatus?.status?.running == true &&
                intervalStatus?.status?.tlName?.equals(clip.name, ignoreCase = true) == true

        val frames = downloadedFrames
        val progress = remember(frames) {
            if (clip.frames > 0 && frames.isNotEmpty()) {
                // Clamp: during a running time-lapse the live frame count can exceed the stale
                // clip.frames, which would otherwise push progress past 1f and hide the controls.
                (frames.size.toFloat() / clip.frames).coerceAtMost(1f)
            } else if (frames.isNotEmpty()) {
                1f // If we don't know total frames but have some downloaded
            } else {
                0f
            }
        }

        val scope = rememberCoroutineScope()
        var cacheSize by remember { mutableStateOf<String?>(null) }
        var lastExportedUri by remember { mutableStateOf<Uri?>(null) }

        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.IO) {
                timelapseViewModel.checkExistingFrames(context, clip.frames)
                val existingFrames = timelapseViewModel.downloadedFrames.value
                if (existingFrames.isNotEmpty()) {
                    val size = timelapseViewModel.getCacheSize(context)
                    cacheSize = formatFileSize(size)
                }
            }
        }

        LaunchedEffect(frames.size) {
            if (frames.isNotEmpty()) {
                val size = timelapseViewModel.getCacheSize(context)
                cacheSize = formatFileSize(size)
            }
        }

        DisposableEffect(key1 = Unit) {
            onDispose {
                timelapseViewModel.togglePlayback(false)
                if (progress > 0f && progress < 1f) {
                    timelapseViewModel.stopDownload()
                }
            }
        }

        LaunchedEffect(isPlaying, currentFrameIndex, frames.size) {
            if (isPlaying && frames.isNotEmpty()) {
                while (isPlaying) {
                    delay(33)
                    // Advance by position in the (possibly gappy) downloaded set, wrapping at the end.
                    val pos = frames.indexOf(currentFrameIndex)
                    val nextFrame = if (pos < 0 || pos >= frames.size - 1) frames.first() else frames[pos + 1]
                    timelapseViewModel.setCurrentFrame(nextFrame)
                }
            }
        }

        LaunchedEffect(Unit) {
            timelapseViewModel.exportResult.collect { result ->
                when (result) {
                    is EncodingResult.Success  -> {
                        EventBus.success("Video exported successfully")
                        lastExportedUri = result.uri
                    }

                    is EncodingResult.Error    -> {
                        EventBus.success("Export failed: ${result.message}")
                    }

                    is EncodingResult.Canceled -> {
                        EventBus.info("Export was canceled")
                    }
                }
            }
        }

        OnConnectedEffect(conn) {
            timelapseViewModel.clipInfo(it)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box {
                TimelapsePlayer(
                    frames = frames,
                    progress = progress,
                    currentFrameIndex = currentFrameIndex,
                    isPlaying = isPlaying,
                    isLive = isLive,
                    isDownloading = isDownloading,
                    context = context,
                    getFrameFile = { index -> timelapseViewModel.getFrameFile(context, index) },
                    onPlayPause = { timelapseViewModel.togglePlayback(!isPlaying) },
                    onSeek = { timelapseViewModel.setCurrentFrame(it) },
                    onScrubStart = { timelapseViewModel.togglePlayback(false) },
                    onGoLive = { timelapseViewModel.goLive() }
                )
                AppBarWithBack(
                    title = clip.name,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    onBackClick = { navigator.pop() },
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (frames.isNotEmpty()) {
                PlayerTimeInfo(frames.size, currentFrameIndex)
            }

            Spacer(modifier = Modifier.height(8.dp))

            PlayerControls(
                progress = progress,
                frames = frames,
                isDownloading = isDownloading,
                onDownload = { timelapseViewModel.downloadFrames(context, conn) },
                onCancelDownload = { timelapseViewModel.stopDownload() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (frames.isNotEmpty()) {
                ExportControls(
                    exportProgress = exportProgress,
                    lastExportedUri = lastExportedUri,
                    onExport = { timelapseViewModel.exportVideo(context) },
                    onCancelExport = { timelapseViewModel.cancelExport() },
                    onOpenVideo = { uri ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, "video/mp4")
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (storedClipInfo != null) {
                InfoView(storedClipInfo!!)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (frames.isNotEmpty() && progress == 1f) {
                Spacer(modifier = Modifier.height(16.dp))
                CacheManagementCard(
                    cacheSize = cacheSize,
                    onDeleteCache = {
                        timelapseViewModel.deleteCache(context)
                        cacheSize = null
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PlayerTimeInfo(framesCount: Int, currentFrameIndex: Int) {
    val fps = 30
    val totalDurationSecs = framesCount / fps
    val currentTimeSecs = currentFrameIndex / fps

    val totalTimeFormatted = formatTime(totalDurationSecs)
    val currentTimeFormatted = formatTime(currentTimeSecs)

    Text(
        text = "Time: $currentTimeFormatted / $totalTimeFormatted at ${fps}fps",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun PlayerControls(
    progress: Float,
    frames: List<Int>,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            // A download is in flight — always offer Cancel, even before the first frame lands.
            isDownloading    -> {
                VButton(
                    text = "Cancel",
                    imageVector = Icons.Default.Close,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { onCancelDownload() }
            }
            // Nothing downloaded yet.
            frames.isEmpty() -> {
                VButton(
                    text = "Download Frames",
                    imageVector = Icons.Default.Download
                ) { onDownload() }
            }
            // Frames available — playback lives on the player overlay now. If the download is
            // incomplete (e.g. stopped), still offer Resume so the user can fetch the rest.
            progress < 1f    -> {
                VButton(
                    text = "Resume Download",
                    imageVector = Icons.Default.Download
                ) { onDownload() }
            }
        }
    }
}

@Composable
fun ExportControls(
    exportProgress: Float,
    lastExportedUri: Uri?,
    onExport: () -> Unit,
    onCancelExport: () -> Unit,
    onOpenVideo: (Uri) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (exportProgress > 0f && exportProgress < 1f) {
            CircularProgressIndicator(progress = { exportProgress })
            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = "Exporting: ${(exportProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.size(4.dp))
            VButton(
                text = "Cancel",
                imageVector = Icons.Default.Close,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                onCancelExport()
            }
        } else {
            VButton(
                text = "Export Video",
                imageVector = Icons.Default.VideoLibrary
            ) {
                onExport()
            }
            Spacer(modifier = Modifier.size(4.dp))
            if (lastExportedUri != null) {
                VButton(
                    text = "Open Video",
                    imageVector = Icons.Outlined.OpenInNew
                ) {
                    onOpenVideo(lastExportedUri)
                }
            }
        }
    }
}

@Composable
fun CacheManagementCard(
    cacheSize: String?,
    onDeleteCache: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cacheSize?.let { "Cache size: $it" } ?: "Calculating cache size...",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            VButton(
                text = "Delete Cache from Smartphone",
                imageVector = Icons.Default.Delete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                onDeleteCache()
            }
        }
    }
}

@Composable
private fun InfoView(info: TimelapseClipInfo) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${info.name} Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                val formattedDate = formatDate(info.date)
                Text(text = formattedDate, color = Color.LightGray)

                Spacer(modifier = Modifier.height(8.dp))

                info.frames?.let { frames ->
                    Text(
                        text = "$frames frames (${calculateDuration(frames, 30)} at 30fps)",
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val shutterValue = info.status.cameraSettings?.shutter ?: "—"
                val apertureValue = info.status.cameraSettings?.aperture ?: "—"
                val isoValue = info.status.cameraSettings?.iso ?: "—"
                Text(
                    text = "Start exposure: $shutterValue f/$apertureValue $isoValue ISO",
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Interval: (${info.program.intervalMode}) day ${info.program.dayInterval}s, night ${info.program.nightInterval}s",
                    color = Color.LightGray
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, HH:mm:ss a")
        localDateTime.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateDuration(frames: Int, fps: Int): String {
    val seconds = frames / fps

    return "${seconds}s"
}

// Removing duplicate formatFileSize function,
// so it will only be here
private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    if (kb < 1024) {
        return String.format("%.2f KB", kb)
    }
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secondsRemaining = seconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining)
    } else {
        String.format("%02d:%02d", minutes, secondsRemaining)
    }
}

@Preview
@Composable
fun InfoViewPreview() {
    MaterialTheme(darkColorScheme()) {
        InfoView(sampleClipInfo)
    }
}

@Preview
@Composable
fun PlayerTimeInfoPreview() {
    MaterialTheme(darkColorScheme()) {
        Surface {
            PlayerTimeInfo(framesCount = 120, currentFrameIndex = 45)
        }
    }
}

@Preview
@Composable
fun PlayerControlsPreview() {
    MaterialTheme(darkColorScheme()) {
        Column {
            PlayerControls(
                progress = 0.5f,
                frames = List(50) { it },
                isDownloading = true,
                onDownload = {},
                onCancelDownload = {}
            )
            PlayerControls(
                progress = 0.5f,
                frames = List(50) { it },
                isDownloading = false,
                onDownload = {},
                onCancelDownload = {}
            )
            PlayerControls(
                progress = 0.0f,
                frames = listOf(),
                isDownloading = false,
                onDownload = {},
                onCancelDownload = {}
            )
        }
    }
}

@Preview
@Composable
fun ExportControlsPreview() {
    MaterialTheme(darkColorScheme()) {
        Column {
            ExportControls(
                exportProgress = 0.35f,
                lastExportedUri = null,
                onExport = {},
                onCancelExport = {},
                onOpenVideo = {}
            )
            ExportControls(
                exportProgress = 0.0f,
                lastExportedUri = null,
                onExport = {},
                onCancelExport = {},
                onOpenVideo = {}
            )
        }
    }
}

@Preview
@Composable
fun CacheManagementCardPreview() {
    MaterialTheme(darkColorScheme()) {
        Surface {
            CacheManagementCard(
                cacheSize = "15.42 MB",
                onDeleteCache = {}
            )
        }
    }
}
