package com.github.naixx.viewapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Minimal YouTube-style scrubber meant to be overlaid on the bottom edge of the player image: a
 * thin track with a draggable thumb, a tall invisible touch target, tap-anywhere-to-seek, and an
 * optional "go live" pill while the time-lapse is still capturing.
 *
 * Driven by *position within [frames]* (the sorted downloaded frame numbers), NOT by raw frame
 * number. Frame numbers aren't necessarily contiguous — a disconnect during a live capture leaves a
 * gap in what's been saved locally — so mapping the thumb by number would desync it from the frame
 * actually on screen. [onSeek] receives a real frame number taken from [frames].
 */
@Composable
fun OverlaySeekBar(
    frames: List<Int>,
    currentFrameIndex: Int,
    isLive: Boolean,
    onScrubStart: () -> Unit,
    onSeek: (Int) -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Companion
) {
    val trackColor = Color.White.copy(alpha = 0.3f)
    val playedColor = MaterialTheme.colorScheme.primary
    val thumbColor = Color.White

    val count = frames.size
    // 0-based position of the shown frame within the downloaded set. indexOf normally hits; the
    // fallback (count of frames at/below it) keeps the thumb sane if currentFrameIndex isn't present.
    val currentPosition = remember(frames, currentFrameIndex) {
        val exact = frames.indexOf(currentFrameIndex)
        if (exact >= 0) exact
        else (frames.count { it <= currentFrameIndex } - 1).coerceIn(0, (count - 1).coerceAtLeast(0))
    }
    val atLiveEdge = count == 0 || currentPosition >= count - 1

    // Read the latest frames/handlers inside the long-lived gesture without restarting pointerInput
    // on every new live frame (which would cancel an in-progress drag).
    val latestFrames by rememberUpdatedState(frames)
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestOnScrubStart by rememberUpdatedState(onScrubStart)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                // Treat the first touch as both a seek (tap-anywhere) and the start of a drag, so
                // scrubbing feels continuous. Starting a drag pauses playback via onScrubStart.
                .pointerInput(Unit) {
                    fun seekTo(x: Float) {
                        val f = latestFrames
                        val w = size.width
                        if (w <= 0 || f.isEmpty()) return
                        val fraction = (x / w).coerceIn(0f, 1f)
                        val pos = (fraction * (f.size - 1)).roundToInt().coerceIn(0, f.size - 1)
                        latestOnSeek(f[pos])
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        latestOnScrubStart()
                        seekTo(down.position.x)
                        drag(down.id) { change ->
                            change.consume()
                            seekTo(change.position.x)
                        }
                    }
                }
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = currentPosition.toFloat(),
                        range = 0f..(count - 1).coerceAtLeast(0).toFloat(),
                        steps = (count - 2).coerceAtLeast(0)
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                val y = size.height / 2f
                val stroke = 3.dp.toPx()
                val fraction =
                    if (count > 1) (currentPosition.toFloat() / (count - 1)).coerceIn(0f, 1f)
                    else 0f
                val thumbX = fraction * size.width
                drawLine(
                    color = trackColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = playedColor,
                    start = Offset(0f, y),
                    end = Offset(thumbX, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(color = thumbColor, radius = 6.dp.toPx(), center = Offset(thumbX, y))
            }
        }

        if (isLive) {
            Spacer(modifier = Modifier.width(8.dp))
            LivePill(atLiveEdge = atLiveEdge, onGoLive = onGoLive)
        }
    }
}

/**
 * "LIVE" indicator/button. Solid red when sitting on the newest captured frame (auto-following),
 * dimmed grey and tappable once the user has scrubbed back or started a playback loop.
 */
@Composable
private fun LivePill(atLiveEdge: Boolean, onGoLive: () -> Unit) {
    val dotColor = if (atLiveEdge) Color.Red else Color.Gray
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(if (!atLiveEdge) Modifier.clickable { onGoLive() } else Companion)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "LIVE",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
fun OverlaySeekBarPreview() {
    MaterialTheme(darkColorScheme()) {
        Surface {
            Column {
                // Finished clip: no LIVE pill.
                OverlaySeekBar(
                    frames = (1..120).toList(),
                    currentFrameIndex = 45,
                    isLive = false,
                    onScrubStart = {},
                    onSeek = {},
                    onGoLive = {}
                )
                // Running, sitting on the newest frame: solid red LIVE.
                OverlaySeekBar(
                    frames = (1..120).toList(),
                    currentFrameIndex = 120,
                    isLive = true,
                    onScrubStart = {},
                    onSeek = {},
                    onGoLive = {}
                )
                // Running, scrubbed back: dimmed, tappable LIVE.
                OverlaySeekBar(
                    frames = (1..120).toList(),
                    currentFrameIndex = 60,
                    isLive = true,
                    onScrubStart = {},
                    onSeek = {},
                    onGoLive = {}
                )
            }
        }
    }
}
