package com.github.naixx.viewapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.*
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

class ClipInfoScreen(val clip: Clip) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = activityViewModel<MainViewModel>()
        val conn by viewModel.connectionState.collectAsState()
        var clipInfo: UiState<TimelapseClipInfo?> by remember { mutableStateOf(UiState.Loading) }
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val downloadProgress by viewModel.downloadProgress.collectAsState()
        val downloadedFrames by viewModel.downloadedFrames.collectAsState()
        val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
        val isPlaying by viewModel.isPlaying.collectAsState()

        val frames = downloadedFrames[clip.name] ?: emptyList()
        val progress = downloadProgress[clip.name] ?: 0f

        val scope = rememberCoroutineScope()
        LL.e("scope = " + scope)
        var cacheSize by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.IO) {
                // Check for existing frames and update progress without starting download
                val existingFrames = viewModel.checkExistingFrames(clip.name, context, clip.frames.toInt())
                if (existingFrames.first > 0 && existingFrames.second > 0) {
                    val size = viewModel.getCacheSize(clip.name, context)
                    cacheSize = formatFileSize(size)
                }
            }
        }

        LaunchedEffect(frames.size) {
            if (frames.isNotEmpty()) {
                val size = viewModel.getCacheSize(clip.name, context)
                cacheSize = formatFileSize(size)
            }
        }

        DisposableEffect(key1 = Unit) {
            onDispose {
                viewModel.togglePlayback(false)
                if (progress > 0f && progress < 1f) {
                    viewModel.stopDownload(clip.name)
                }
            }
        }

        LaunchedEffect(isPlaying, currentFrameIndex, frames.size) {
            if (isPlaying && frames.isNotEmpty()) {
                while (isPlaying) {
                    delay(33)
                    val nextFrame = if (currentFrameIndex >= frames.size) 1 else currentFrameIndex + 1
                    viewModel.setCurrentFrame(nextFrame)
                }
            }
        }

        OnConnectedEffect(conn) {
            clipInfo = viewModel.clipInfo(clip)
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
                    .background(Color.Black)
                    ,
                contentAlignment = Alignment.Center
            ) {

                var currentPainter by remember { mutableStateOf<Painter?>(null) }
                if (progress > 0f && frames.isNotEmpty() && currentFrameIndex > 0 && currentFrameIndex <= frames.size) {
                    val frameFile = viewModel.getFrameFile(clip.name, currentFrameIndex, context)
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
                            ?: if (progress > 0 && frames.isNotEmpty()) (frames.size / progress).toInt() else 0

                        if (totalFrames > 0) {
                            Text(
                                text = "Downloaded $downloadedCount of $totalFrames frames (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "Downloaded ${frames.size} frames (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                Text(
                    text = "Frame: $currentFrameIndex of ${frames.size}",
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
                            viewModel.setCurrentFrame(it.toInt().coerceIn(1, frames.size))
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
                        if (!viewModel.isDownloading(clip.name))
                            VButton(
                                text = "Resume Download",
                                imageVector = Icons.Default.PlayArrow
                            ) {
                                viewModel.downloadFrames(clip.name, context)
                            }
                        else
                            VButton(
                                text = "Cancel",
                                imageVector = Icons.Default.Close,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) { viewModel.stopDownload(clip.name) }
                    }
                } else if (frames.isEmpty()) {
                    VButton(
                        text = "Download Frames",
                        imageVector = Icons.Default.Download
                    ) {
                        viewModel.downloadFrames(clip.name, context)
                    }
                } else if (progress == 1f) {
                    VButton(
                        text = if (isPlaying) "Pause" else "Play",
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    ) { viewModel.togglePlayback(!isPlaying) }

                    Spacer(modifier = Modifier.width(16.dp))

                    VButton(
                        text = "Reset"
                    ) { viewModel.setCurrentFrame(1) }
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
                            viewModel.deleteCache(clip.name, context)
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

@Preview(showSystemUi = true)
@Composable
fun InfoViewPreview() {
    MaterialTheme(darkColorScheme()) {
        InfoView(sampleClipInfo)
    }
}
