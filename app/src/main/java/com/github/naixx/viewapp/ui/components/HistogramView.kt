package com.github.naixx.viewapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun HistogramView(values: List<Int>?, modifier: Modifier = Modifier) {
    values?.takeIf { it.isNotEmpty() }?.let {
        val color = MaterialTheme.colorScheme.onSurface
        Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
            val barWidth = size.width / it.size
            val maxValue = it.maxOrNull()?.coerceAtLeast(1) ?: 1

            it.forEachIndexed { index, value ->
                val height = (value.toFloat() / maxValue) * size.height
                drawRect(
                    color = color,
                    topLeft = Offset(index * barWidth, size.height - height),
                    size = Size(barWidth, height)
                )
            }
        }
    }
}

/**
 * Histogram of the frame the player is showing, so it follows scrubbing and playback. Keeps the previous
 * frame's histogram on screen until the new one is ready.
 */
@Composable
fun FrameHistogramView(frameIndex: Int, modifier: Modifier = Modifier, histogramOf: suspend (Int) -> List<Int>?) {
    val values by produceState<List<Int>?>(null, frameIndex) { value = histogramOf(frameIndex) }
    HistogramView(values, modifier)
}
