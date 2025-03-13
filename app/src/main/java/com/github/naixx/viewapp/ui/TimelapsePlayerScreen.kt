package com.github.naixx.viewapp.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.Clip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class TimelapsePlayerScreen(val clip: Clip) : Screen {

    @Composable
    override fun Content() {
        val viewModel = activityViewModel<MainViewModel>()
        val context = LocalContext.current
        val downloadProgress by viewModel.downloadProgress.collectAsState()
        val downloadedFrames by viewModel.downloadedFrames.collectAsState()
        val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
        val isPlaying by viewModel.isPlaying.collectAsState()

        val frames = downloadedFrames[clip.name] ?: emptyList()
        val progress = downloadProgress[clip.name] ?: 0f

        val scope = rememberCoroutineScope()
        var cacheSize by remember { mutableStateOf<String?>(null) }

        // Get cache size
        LaunchedEffect(frames.size) {
            if (frames.isNotEmpty()) {
                val size = viewModel.getCacheSize(clip.name, context)
                cacheSize = formatFileSize(size)
            }
        }

        // Stop downloads and playback when leaving the screen
        DisposableEffect(key1 = Unit) {
            onDispose {
                viewModel.togglePlayback(false)
                if (progress > 0f && progress < 1f) {
                    viewModel.stopDownload(clip.name)
                }
            }
        }

        // Playback effect - advances frames during playback
        LaunchedEffect(isPlaying, currentFrameIndex, frames.size) {
            if (isPlaying && frames.isNotEmpty()) {
                while (isPlaying) {
                    delay(33) // ~30fps
                    val nextFrame = if (currentFrameIndex >= frames.size) 1 else currentFrameIndex + 1
                    viewModel.setCurrentFrame(nextFrame)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "${clip.name} Timelapse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frame display area - shows the current frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f/3f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                var currentPainter by remember { mutableStateOf<Painter?>(null) }
                if (progress > 0f && frames.isNotEmpty() && currentFrameIndex > 0 && currentFrameIndex <= frames.size) {
                    // Show current frame
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
                } else {
                    // Show loading or instructions
                    if (progress > 0f && progress < 1f) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(progress = progress)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Downloading: ${(progress * 100).toInt()}%",
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "Press Download to begin",
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frame slider - lets user scrub through frames
            if (frames.isNotEmpty()) {
                Text(
                    text = "Frame: $currentFrameIndex of ${frames.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Control buttons (download/play/pause)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (frames.isEmpty() || progress == 0f) {
                    // Download button if no frames downloaded or download was canceled
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.downloadFrames(clip.name, context)
                            }
                        },
                        enabled = progress == 0f  // Only enable if not currently downloading
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Download",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Frames")
                    }
                } else if (progress == 1f) {
                    // Play/pause button once frames are downloaded
                    Button(
                        onClick = { viewModel.togglePlayback(!isPlaying) }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Menu else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isPlaying) "Pause" else "Play")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Reset to first frame button
                    OutlinedButton(
                        onClick = { viewModel.setCurrentFrame(1) }
                    ) {
                        Text("Reset")
                    }
                }
            }

            // Download progress text
            if (progress > 0f && progress < 1f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading: ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Cancel download button
                    Button(
                        onClick = { viewModel.stopDownload(clip.name) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            }

            // Show cache management when frames are downloaded
            if (frames.isNotEmpty() && progress == 1f) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cache Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = cacheSize?.let { "Cache size: $it" } ?: "Calculating cache size...",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.deleteCache(clip.name, context)
                                    cacheSize = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Cache",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Cache")
                        }
                    }
                }
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        if (kb < 1024) {
            return String.format("%.2f KB", kb)
        }
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
