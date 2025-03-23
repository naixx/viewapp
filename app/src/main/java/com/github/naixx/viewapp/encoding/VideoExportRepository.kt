package com.github.naixx.viewapp.encoding

import android.content.Context
import android.net.Uri
import com.github.naixx.logger.LL
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

interface VideoExportRepository {
    val progress: StateFlow<Float>
    suspend fun exportVideo(frames: List<File>, outputFileName: String, fps: Int = VideoEncoder.DEFAULT_FPS): EncodingResult
    fun cancelExport()
}

class VideoExportRepositoryImpl(
    private val context: Context,
    private val videoEncoderFactory: VideoEncoderFactory
) : VideoExportRepository {

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress

    private var currentEncoder: VideoEncoder? = null

    override suspend fun exportVideo(frames: List<File>, outputFileName: String, fps: Int): EncodingResult {
        if (frames.isEmpty()) {
            return EncodingResult.Error("No frames to export")
        }

        // Reset progress
        _progress.value = 0f

        // Generate output path
        val outputPath = "${context.cacheDir.absolutePath}/export/$outputFileName"
        val outputDir = File(outputPath).parentFile
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Get encoder
        val encoder = videoEncoderFactory.createEncoder().also {
            currentEncoder = it
        }

        try {
            // Start encoding
            val result = encoder.encodeFramesToVideo(frames, outputPath, fps) { progress ->
                _progress.value = progress
            }

            // Always reset progress after completion
            _progress.value = 0f

            return result
        } catch (e: Exception) {
            LL.e("Error during video export: ${e.message}")
            _progress.value = 0f
            return EncodingResult.Error("Export failed: ${e.message}", e)
        }
    }

    override fun cancelExport() {
        currentEncoder?.cancel()
        _progress.value = 0f
    }
}
