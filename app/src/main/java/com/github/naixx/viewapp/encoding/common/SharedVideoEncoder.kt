package com.github.naixx.viewapp.encoding.common

import com.github.naixx.viewapp.encoding.EncodingResult
import com.github.naixx.viewapp.encoding.VideoEncoder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException

/**
 * Placeholder for a future implementation that could be shared across platforms
 * For example using FFmpeg via Kotlin/Native bindings
 */
abstract class SharedVideoEncoder : VideoEncoder {
    protected val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress

    protected val _result = MutableSharedFlow<EncodingResult>(extraBufferCapacity = 1)
    override val result: SharedFlow<EncodingResult> = _result

    protected var isCancelled = false

    override fun cancel() {
        isCancelled = true
    }

    /**
     * Common validation logic for encoding operations
     */
    protected fun validateFrames(frames: List<File>): EncodingResult? {
        if (frames.isEmpty()) {
            return EncodingResult.Error("No frames to encode")
        }

        val missingFrames = frames.filterNot { it.exists() }
        if (missingFrames.isNotEmpty()) {
            return EncodingResult.Error("${missingFrames.size} frames are missing")
        }

        return null
    }

    /**
     * Ensures the output directory exists
     */
    @Throws(IOException::class)
    protected fun ensureOutputDirectoryExists(outputPath: String) {
        val outputFile = File(outputPath)
        val parentDir = outputFile.parentFile ?: throw IOException("Invalid output path: $outputPath")

        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw IOException("Failed to create output directory: ${parentDir.absolutePath}")
        }
    }
}
