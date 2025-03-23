package com.github.naixx.viewapp.encoding.android

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.encoding.EncodingResult
import com.github.naixx.viewapp.encoding.VideoEncoder
import com.github.naixx.viewapp.encoding.VideoEncodingParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.coroutineContext

class MediaCodecEncoder(
    private val context: Context
) : VideoEncoder {

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress

    private val _result = MutableSharedFlow<EncodingResult>(extraBufferCapacity = 1)
    override val result: SharedFlow<EncodingResult> = _result

    private var isEncodingCancelled = false

    override suspend fun encodeFramesToVideo(
        frames: List<File>,
        outputFilePath: String,
        fps: Int,
        onProgressUpdate: suspend (Float) -> Unit
    ): EncodingResult = withContext(Dispatchers.IO) {
        if (frames.isEmpty()) {
            return@withContext EncodingResult.Error("No frames to encode")
        }

        isEncodingCancelled = false
        _progress.value = 0f

        try {
            val fileName = outputFilePath.substringAfterLast('/')
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                }
            }

            val resolver = context.contentResolver
            val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext EncodingResult.Error("Failed to create video file")

            try {
                val tempVideoFile = File(context.cacheDir, "temp_${fileName}")
                if (tempVideoFile.exists()) {
                    tempVideoFile.delete()
                }

                val firstFrameFile = frames.first()
                if (!firstFrameFile.exists()) {
                    return@withContext EncodingResult.Error("Cannot access first frame")
                }

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(firstFrameFile.path, options)
                val width = options.outWidth
                val height = options.outHeight

                val encodingParams = VideoEncodingParameters(
                    width = width,
                    height = height,
                    bitRate = width * height * 4,
                    frameRate = fps
                )

                val result = encodeWithMediaCodec(frames, tempVideoFile.absolutePath, encodingParams, onProgressUpdate)

                if (result is EncodingResult.Success) {
                    resolver.openOutputStream(videoUri)?.use { outputStream ->
                        FileInputStream(tempVideoFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    tempVideoFile.delete()

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(videoUri.toString()),
                        arrayOf("video/mp4"),
                        null
                    )

                    _progress.value = 0f // Reset progress after success
                    val successResult = EncodingResult.Success(outputFilePath, videoUri)
                    _result.emit(successResult)
                    return@withContext successResult
                } else {
                    resolver.delete(videoUri, null, null)
                    val errorResult = result as? EncodingResult.Error ?: EncodingResult.Error("Unknown error during encoding")
                    _result.emit(errorResult)
                    return@withContext errorResult
                }
            } catch (e: Exception) {
                LL.e("Video export error: ${e.message}")
                resolver.delete(videoUri, null, null)
                val errorResult = EncodingResult.Error("Export failed: ${e.message}", e)
                _result.emit(errorResult)
                return@withContext errorResult
            }
        } catch (e: Exception) {
            LL.e("Export error: ${e.message}")
            val errorResult = EncodingResult.Error("Export failed: ${e.message}", e)
            _result.emit(errorResult)
            return@withContext errorResult
        }
    }

    private suspend fun encodeWithMediaCodec(
        frames: List<File>,
        outputPath: String,
        params: VideoEncodingParameters,
        onProgressUpdate: suspend (Float) -> Unit
    ): EncodingResult {
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, params.width, params.height)
        format.setInteger(MediaFormat.KEY_BIT_RATE, params.bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, params.frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, params.iFrameInterval)

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            // Find a suitable encoder and color format
            var selectedColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

            // Try common YUV420 formats that most devices support
            val preferredFormats = listOf(
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )

            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoderNames = ArrayList<String>()

            // Find compatible encoder and format
            for (info in codecList.codecInfos) {
                if (!info.isEncoder || !info.supportedTypes.contains(mimeType)) {
                    continue
                }

                encoderNames.add(info.name)

                try {
                    val caps = info.getCapabilitiesForType(mimeType)
                    val colorFormats = caps.colorFormats

                    // Check if any of our preferred formats are supported
                    for (preferredFormat in preferredFormats) {
                        if (colorFormats.contains(preferredFormat)) {
                            selectedColorFormat = preferredFormat
                            LL.d("Using encoder ${info.name} with format $selectedColorFormat")
                            break
                        }
                    }
                } catch (e: Exception) {
                    LL.e("Error checking encoder ${info.name}: ${e.message}")
                }
            }

            LL.d("Available encoders: ${encoderNames.joinToString()}")
            LL.d("Using color format: $selectedColorFormat")

            // Set the selected format
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, selectedColorFormat)

            // Create and configure encoder
            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var presentationTimeUs = 0L
            val frameDurationUs = 1_000_000L / params.frameRate

            frames.forEachIndexed { index, frameFile ->
                if (!coroutineContext.isActive || isEncodingCancelled) {
                    return EncodingResult.Canceled
                }

                if (frameFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(frameFile.path)
                    if (bitmap == null) {
                        LL.e("Failed to decode bitmap from file: ${frameFile.path}")
                        return@forEachIndexed
                    }

                    // Use our proper color converter
                    val inputData = ColorConverter.convertBitmapToI420(bitmap)

                    var inputBufferIndex = encoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer == null) {
                            LL.e("Failed to get input buffer at index $inputBufferIndex")
                            return@forEachIndexed
                        }

                        inputBuffer.clear()
                        inputBuffer.put(inputData)

                        encoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            inputData.size,
                            presentationTimeUs,
                            0
                        )

                        presentationTimeUs += frameDurationUs
                    }

                    var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    while (outputBufferIndex >= 0) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }

                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    }

                    bitmap.recycle()
                }

                val progress = (index + 1f) / frames.size
                _progress.value = progress
                onProgressUpdate(progress)
            }

            val inputBufferIndex = encoder.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                encoder.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            while (outputBufferIndex >= 0) {
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }

                if (muxerStarted && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }

                encoder.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            }

            return EncodingResult.Success(outputPath)
        } catch (e: Exception) {
            _progress.value = 0f
            LL.e(e, "MediaCodec encoding error: ${e.message}")
            return EncodingResult.Error("Encoding failed: ${e.message}", e)
        } finally {
            try {
                encoder?.stop()
                encoder?.release()

                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (e: Exception) {
                LL.e(e, "Error cleaning up resources: ${e.message}")
            }
        }
    }

    override fun cancel() {
        isEncodingCancelled = true
        _progress.value = 0f
    }
}
