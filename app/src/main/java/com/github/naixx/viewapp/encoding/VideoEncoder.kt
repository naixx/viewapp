package com.github.naixx.viewapp.encoding

import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface VideoEncoder {
    val progress: StateFlow<Float>
    val result: SharedFlow<EncodingResult>

    suspend fun encodeFramesToVideo(
        frames: List<File>,
        outputFilePath: String,
        fps: Int,
        onProgressUpdate: suspend (Float) -> Unit
    ): EncodingResult

    fun cancel()

    companion object {
        const val DEFAULT_FPS = 30
    }
}

sealed class EncodingResult {
    data class Success(val outputPath: String, val uri: Uri? = null) : EncodingResult()
    data class Error(val message: String, val exception: Throwable? = null) : EncodingResult()
    object Canceled : EncodingResult()
}

data class VideoEncodingParameters(
    val width: Int,
    val height: Int,
    val bitRate: Int,
    val frameRate: Int,
    val iFrameInterval: Int = 1
)
