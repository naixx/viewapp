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
import com.github.naixx.compose.*
import com.github.naixx.viewapp.encoding.EncodingResult
import com.github.naixx.viewapp.ui.components.AppBarWithBack
import com.github.naixx.viewapp.ui.components.OnConnectedEffect
import com.github.naixx.viewapp.ui.components.VButton
import com.github.naixx.viewapp.ui.components.sampleClipInfo
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
        var clipInfo: UiState<TimelapseClipInfo?> by remember { mutableStateOf(UiState.Loading) }
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
            clipInfo = timelapseViewModel.clipInfo(it)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // Timelapse player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(160f / 106f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {

                var currentPainter by remember { mutableStateOf<Painter?>(null) }
                if (progress > 0f && frames.isNotEmpty() && currentFrameIndex > 0 && currentFrameIndex <= frames.size) {
                    val frameFile = timelapseViewModel.getFrameFile(context, currentFrameIndex)
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(progress = { progress })
                        Spacer(modifier = Modifier.height(8.dp))
                        val downloadedCount = frames.size
                        val totalFrames = clip.frames.takeIf { it > 0 }
                            ?: if (frames.isNotEmpty()) frames.size else 0

                        if (totalFrames > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Downloaded $downloadedCount of $totalFrames frames (${(progress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Downloaded ${frames.size} frames (${(progress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else if (progress < 1f) {
                    Text(
                        text = "Press Download to begin",
                        color = Color.White
                    )
                }

                AppBarWithBack(
                    title = clip.name,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    onBackClick = { navigator.pop() },
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }



            Spacer(modifier = Modifier.height(8.dp))

            // Player controls
            if (frames.isNotEmpty()) {
                val fps = 30 // Default framerate for estimation
                val totalDurationSecs = frames.size / fps
                val currentTimeSecs = currentFrameIndex / fps

                val totalTimeFormatted = formatTime(totalDurationSecs)
                val currentTimeFormatted = formatTime(currentTimeSecs)

                Text(
                    text = "Frame: $currentFrameIndex of ${frames.size}",
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

                Spacer(modifier = Modifier.height(8.dp))

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
                            timelapseViewModel.setCurrentFrame(it.toInt().coerceIn(1, frames.size))
                        },
                        valueRange = 1f..frames.size.toFloat(),
                        steps = if (frames.size > 2) frames.size - 2 else 0,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${frames.size}",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress > 0f && progress < 1f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!timelapseViewModel.isDownloading())
                            VButton(
                                text = "Resume Download",
                                imageVector = Icons.Default.PlayArrow
                            ) {
                                timelapseViewModel.downloadFrames(context, conn)
                            }
                        else
                            VButton(
                                text = "Cancel",
                                imageVector = Icons.Default.Close,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) { timelapseViewModel.stopDownload() }
                    }
                } else if (frames.isEmpty()) {
                    VButton(
                        text = "Download Frames",
                        imageVector = Icons.Default.Download
                    ) {
                        timelapseViewModel.downloadFrames(context, conn)
                    }
                } else if (progress == 1f) {
                    VButton(
                        text = if (isPlaying) "Pause" else "Play",
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    ) { timelapseViewModel.togglePlayback(!isPlaying) }

                    Spacer(modifier = Modifier.width(16.dp))

                    VButton(
                        text = "Reset"
                    ) { timelapseViewModel.setCurrentFrame(1) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Video button
            if (frames.isNotEmpty()) {
                Spacer(modifier = Modifier.width(16.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {

                    if (exportProgress > 0f && exportProgress < 1f) {
                        // Show export progress

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
                            timelapseViewModel.cancelExport()
                        }
                    } else {
                        VButton(
                            text = "Export Video",
                            imageVector = Icons.Default.VideoLibrary
                        ) {
                            timelapseViewModel.exportVideo(context)
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        if (lastExportedUri != null) {
                            VButton(
                                text = "Open Video",
                                imageVector = Icons.Outlined.OpenInNew
                            ) {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(lastExportedUri, "video/mp4")
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clip info card
            clipInfo.Render({
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading clip info...")
                }
            }) {
                it?.let { info ->
                    InfoView(info)
                }
            }

            // Cache Management Card
            if (frames.isNotEmpty() && progress == 1f) {
                Spacer(modifier = Modifier.height(16.dp))
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
                            timelapseViewModel.deleteCache(context)
                            cacheSize = null
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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

@Preview(showSystemUi = true)
@Composable
fun InfoViewPreview() {
    MaterialTheme(darkColorScheme()) {
        InfoView(sampleClipInfo)
    }
}
