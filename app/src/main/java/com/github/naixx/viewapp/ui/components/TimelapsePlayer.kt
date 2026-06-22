package com.github.naixx.viewapp.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.crossfade
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelapsePlayer(
    frames: List<Int>,
    progress: Float,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    isLive: Boolean,
    isDownloading: Boolean = false,
    context: Context,
    getFrameFile: (Int) -> File?,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onScrubStart: () -> Unit,
    onGoLive: () -> Unit
) {
    // The togglable overlay = gray scrim + center play/pause button. The thin seek bar at the bottom
    // stays pinned regardless. YouTube model: visible on start, auto-hides a few seconds into
    // playback, reappears on tap, stays put while paused.
    var controlsVisible by remember { mutableStateOf(true) }
    val hasFrames = frames.isNotEmpty() && progress > 0f

    // Pausing always brings the controls back; once playback resumes they linger briefly, then fade.
    LaunchedEffect(isPlaying) {
        if (!isPlaying) controlsVisible = true
    }
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(160f / 106f)
            .background(Color.Black)
            // Tap the bare image to toggle the overlay. The seek bar and the center button consume
            // their own taps first, so this only fires for taps on the empty image area.
            .pointerInput(Unit) {
                detectTapGestures { controlsVisible = !controlsVisible }
            },
        contentAlignment = Alignment.Center
    ) {
        var currentPainter by remember { mutableStateOf<Painter?>(null) }

        // Render whatever frame currentFrameIndex points at as long as its file exists. Do NOT gate on
        // `currentFrameIndex <= frames.size`: currentFrameIndex is a frame *number*, which can exceed
        // the downloaded *count* when frames aren't contiguous (e.g. live capture with gaps), and that
        // mismatch would hide the newest live frame. The getFrameFile null-check below is the real guard.
        if (progress > 0f && frames.isNotEmpty() && currentFrameIndex > 0) {
            val frameFile = getFrameFile(currentFrameIndex)
            if (frameFile != null) {
                AsyncImage(
                    model = Builder(context)
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

        // Scrim + center button shown together; never while downloading or before any frame.
        if (hasFrames && !isDownloading && controlsVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            CenterPlayButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isDownloading) {
            // Only overlay the spinner while actively downloading. Once stopped, show the frames so a
            // partial download stays playable instead of being covered or blanked out.
            DownloadProgressOverlay(progress, frames.size)
        } else if (frames.isEmpty()) {
            Text(
                text = "Press Download to begin",
                color = Color.White
            )
        }

        // Always-pinned thin seek bar, over a subtle gradient so it stays legible on bright frames.
        if (hasFrames) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            ) {
                OverlaySeekBar(
                    frames = frames,
                    currentFrameIndex = currentFrameIndex,
                    isLive = isLive,
                    onScrubStart = onScrubStart,
                    onSeek = onSeek,
                    onGoLive = onGoLive
                )
            }
        }
    }
}

@Composable
private fun CenterPlayButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Companion) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun DownloadProgressOverlay(progress: Float, downloadedCount: Int) {
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
