package com.github.naixx.viewapp.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.*
import coil3.compose.AsyncImage
import coil3.request.*
import com.github.naixx.viewapp.encoding.EncodingResult
import com.github.naixx.viewapp.ui.components.*
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.*
import kotlinx.coroutines.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.*
import java.time.format.DateTimeFormatter

class ClipInfoScreen(val clip: Clip) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val mainViewModel = activityViewModel<MainViewModel>()
        val timelapseViewModel = koinViewModel<TimelapseViewModel>(parameters = { parametersOf(clip) })
        val conn by mainViewModel.connectionState.collectAsState()
        val storedClipInfo by timelapseViewModel.clipInfo.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val downloadedFrames by timelapseViewModel.downloadedFrames.collectAsState()
        val currentFrameIndex by timelapseViewModel.currentFrameIndex.collectAsState()
        val isPlaying by timelapseViewModel.isPlaying.collectAsState()
        val exportProgress by timelapseViewModel.exportProgress.collectAsState()

        val frames = downloadedFrames
        val progress = remember(frames) {
            if (clip.frames > 0 && frames.isNotEmpty()) {
                frames.size.toFloat() / clip.frames
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
                val existingFrames = timelapseViewModel.checkExistingFrames(context)
                if (existingFrames.first > 0 && existingFrames.second > 0) {
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
                    val nextFrame = if (currentFrameIndex >= frames.size) 1 else currentFrameIndex + 1
                    timelapseViewModel.setCurrentFrame(nextFrame)
                }
            }
        }

        LaunchedEffect(Unit) {
            timelapseViewModel.exportResult.collect { result ->
                when (result) {
                    is EncodingResult.Success -> {
                        Toast.makeText(context, "Video exported successfully", Toast.LENGTH_LONG).show()
                        lastExportedUri = result.uri
                    }

                    is EncodingResult.Error -> {
                        Toast.makeText(context, "Export failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }

                    is EncodingResult.Canceled -> {
                        Toast.makeText(context, "Export was canceled", Toast.LENGTH_LONG).show()
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
                .verticalScroll(rememberScrollState())
        ) {
            TimelapsePlayer(
                frames = frames,
                progress = progress,
                currentFrameIndex = currentFrameIndex,
                context = context,
                title = clip.name,
                onBackClick = { navigator.pop() },
                getFrameFile = { index -> timelapseViewModel.getFrameFile(context, index) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (frames.isNotEmpty()) {
                PlayerTimeInfo(frames.size, currentFrameIndex)

                Spacer(modifier = Modifier.height(8.dp))

                PlayerSeekBar(
                    currentFrameIndex = currentFrameIndex,
                    framesCount = frames.size,
                    onFrameSelected = { timelapseViewModel.setCurrentFrame(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelapsePlayer(
    frames: List<Int>,
    progress: Float,
    currentFrameIndex: Int,
    context: android.content.Context,
    title: String,
    onBackClick: () -> Unit,
    getFrameFile: (Int) -> java.io.File?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(160f / 106f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        var currentPainter by remember { mutableStateOf<Painter?>(null) }

        if (progress > 0f && frames.isNotEmpty() && currentFrameIndex > 0 && currentFrameIndex <= frames.size) {
            val frameFile = getFrameFile(currentFrameIndex)
            if (frameFile != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(frameFile)
                        .crossfade(false)
                        .build(),
                    contentDescription = "Frame $currentFrameIndex",
                    placeholder = currentPainter,
                    onSuccess = { painter ->
                        currentPainter = painter.painter
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (progress > 0f && progress < 1f) {
            DownloadProgressOverlay(progress, frames.size)
        } else if (progress < 1f) {
            Text(
                text = "Press Download to begin",
                color = Color.White
            )
        }

        AppBarWithBack(
            title = title,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
fun DownloadProgressOverlay(progress: Float, downloadedCount: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(progress = { progress })
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Downloaded $downloadedCount frames (${(progress * 100).toInt()}%)",
            style = MaterialTheme.typography.bodyMedium
        )
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
        text = "Frame: $currentFrameIndex of $framesCount",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        textAlign = TextAlign.Center
    )

    Text(
        text = "Time: $currentTimeFormatted / $totalTimeFormatted at ${fps}fps",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun PlayerSeekBar(
    currentFrameIndex: Int,
    framesCount: Int,
    onFrameSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "1",
            modifier = Modifier.padding(end = 8.dp)
        )

        Slider(
            value = currentFrameIndex.toFloat(),
            onValueChange = {
                onFrameSelected(it.toInt().coerceIn(1, framesCount))
            },
            valueRange = 1f..framesCount.toFloat(),
            steps = if (framesCount > 2) framesCount - 2 else 0,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$framesCount",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun PlayerControls(
    progress: Float,
    frames: List<Int>,
    isPlaying: Boolean,
    isDownloading: Boolean,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (progress > 0f && progress < 1f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isDownloading)
                    VButton(
                        text = "Resume Download",
                        imageVector = Icons.Default.PlayArrow
                    ) {
                        onDownload()
                    }
                else
                    VButton(
                        text = "Cancel",
                        imageVector = Icons.Default.Close,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { onCancelDownload() }
            }
        } else if (frames.isEmpty()) {
            VButton(
                text = "Download Frames",
                imageVector = Icons.Default.Download
            ) {
                onDownload()
            }
        } else if (progress == 1f) {
            VButton(
                text = if (isPlaying) "Pause" else "Play",
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
            ) { onPlayPause() }

            Spacer(modifier = Modifier.width(16.dp))

            VButton(
                text = "Reset"
            ) { onReset() }
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
                text = "Delete Cache",
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

                val shutterValue = info.status.cameraSettings.shutter
                val apertureValue = info.status.cameraSettings.aperture
                val isoValue = info.status.cameraSettings.iso
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
fun PlayerSeekBarPreview() {
    MaterialTheme(darkColorScheme()) {
        Surface {
            PlayerSeekBar(
                currentFrameIndex = 45,
                framesCount = 120,
                onFrameSelected = {}
            )
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
                isPlaying = false,
                isDownloading = true,
                onPlayPause = {},
                onReset = {},
                onDownload = {},
                onCancelDownload = {}
            )
            PlayerControls(
                progress = 0.0f,
                frames = List(50) { it },
                isPlaying = false,
                isDownloading = true,
                onPlayPause = {},
                onReset = {},
                onDownload = {},
                onCancelDownload = {}
            )
            PlayerControls(
                progress = 0.0f,
                frames = listOf(),
                isPlaying = false,
                isDownloading = true,
                onPlayPause = {},
                onReset = {},
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
