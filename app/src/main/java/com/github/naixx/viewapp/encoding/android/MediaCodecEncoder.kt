package com.github.naixx.viewapp.encoding.android

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
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
import github.naixx.core.FrameStore
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
import kotlin.math.roundToInt

class MediaCodecEncoder(
    private val context: Context,
    private val store: FrameStore,
) : VideoEncoder {

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress

    private val _result = MutableSharedFlow<EncodingResult>(extraBufferCapacity = 1)
    override val result: SharedFlow<EncodingResult> = _result

    private var isEncodingCancelled = false

    override suspend fun encodeFramesToVideo(
        clip: String,
        frames: List<Int>,
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

                val firstFrame = store.read(clip, frames.first())
                    ?: return@withContext EncodingResult.Error("Cannot access first frame")

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(firstFrame, 0, firstFrame.size, options)
                val width = options.outWidth
                val height = options.outHeight

                val encodingParams = VideoEncodingParameters(
                    width = width,
                    height = height,
                    bitRate = width * height * 4,
                    frameRate = fps
                )

                val result = encodeWithMediaCodec(clip, frames, tempVideoFile.absolutePath, encodingParams, onProgressUpdate)

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
        clip: String,
        frames: List<Int>,
        outputPath: String,
        params: VideoEncodingParameters,
        onProgressUpdate: suspend (Float) -> Unit
    ): EncodingResult {
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            // Create the encoder we will actually use FIRST, then derive the
            // format from *its* capabilities. The previous code scanned every
            // encoder for one supporting COLOR_FormatYUV420Planar but then let
            // createEncoderByType() pick the device's default encoder, which on
            // many devices is a different (hardware) encoder that does not
            // support Planar -> configure() throws IllegalArgumentException.
            // This is why it worked on the emulator (single software encoder)
            // but failed on a Pixel 5 (Qualcomm hardware encoder).
            val codec = MediaCodec.createEncoderByType(mimeType)
            encoder = codec

            val videoCaps = codec.codecInfo
                .getCapabilitiesForType(mimeType)
                .videoCapabilities

            // Pick a size the encoder actually supports. Frames stay at native
            // size when possible; tiny device previews (e.g. 160x106) are scaled
            // up to the encoder's minimum, oversized frames are scaled down -
            // always preserving aspect ratio, then aligned to the encoder's
            // required multiple. Hardware encoders reject sizes below their
            // minimum (commonly 128x128) or off their alignment grid.
            val (width, height) = computeTargetSize(params.width, params.height, videoCaps)

            val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, params.bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, params.frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, params.iFrameInterval)
                // Flexible YUV420 is mandated on every encoder since API 22
                // (minSdk is 24), and getInputImage() lets us write planes using
                // the device's real strides/layout (planar vs. semiplanar)
                // instead of guessing the color format.
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
            }

            LL.i("Encoding with ${codec.name} at ${width}x$height (requested ${params.width}x${params.height})")

            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            var presentationTimeUs = 0L
            val frameDurationUs = 1_000_000L / params.frameRate
            val inputSize = width * height * 3 / 2

            frames.forEachIndexed { index, frameIndex ->
                if (!coroutineContext.isActive || isEncodingCancelled) {
                    return EncodingResult.Canceled
                }

                val jpeg = store.read(clip, frameIndex)
                if (jpeg != null) {
                    val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                    if (bitmap == null) {
                        LL.e("Failed to decode frame $frameIndex of $clip")
                        return@forEachIndexed
                    }

                    val frame = if (bitmap.width == width && bitmap.height == height) {
                        bitmap
                    } else {
                        Bitmap.createScaledBitmap(bitmap, width, height, true)
                    }

                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val image = codec.getInputImage(inputBufferIndex)
                        if (image != null) {
                            ColorConverter.fillInputImage(image, frame)
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                inputSize,
                                presentationTimeUs,
                                0
                            )
                            presentationTimeUs += frameDurationUs
                        } else {
                            LL.e("getInputImage returned null at index $inputBufferIndex")
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, 0)
                        }
                    }

                    if (frame !== bitmap) frame.recycle()

                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    while (outputBufferIndex >= 0) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }

                        val encodedData = codec.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    }

                    bitmap.recycle()
                }

                val progress = (index + 1f) / frames.size
                _progress.value = progress
                onProgressUpdate(progress)
            }

            val inputBufferIndex = codec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                codec.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            while (outputBufferIndex >= 0) {
                if (muxerStarted && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    val encodedData = codec.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
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

    /**
     * Choose output dimensions the AVC encoder supports for a source frame of
     * [srcW] x [srcH]: scale (preserving aspect ratio) to fit within the
     * encoder's supported width/height range, then align to its required
     * multiples. Frames already within range keep their native size.
     */
    private fun computeTargetSize(
        srcW: Int,
        srcH: Int,
        caps: MediaCodecInfo.VideoCapabilities?
    ): Pair<Int, Int> {
        if (caps == null) {
            // Unknown capabilities: fall back to even dimensions (YUV 4:2:0 needs them).
            return (srcW and 1.inv()) to (srcH and 1.inv())
        }
        val minW = caps.supportedWidths.lower
        val maxW = caps.supportedWidths.upper
        val minH = caps.supportedHeights.lower
        val maxH = caps.supportedHeights.upper

        var scale = 1.0
        if (srcW < minW) scale = maxOf(scale, minW.toDouble() / srcW)
        if (srcH < minH) scale = maxOf(scale, minH.toDouble() / srcH)
        if (srcW * scale > maxW) scale = minOf(scale, maxW.toDouble() / srcW)
        if (srcH * scale > maxH) scale = minOf(scale, maxH.toDouble() / srcH)

        val w = alignToRange((srcW * scale).roundToInt(), caps.widthAlignment, minW, maxW)
        val h = alignToRange((srcH * scale).roundToInt(), caps.heightAlignment, minH, maxH)
        return w to h
    }

    /** Clamp [value] into [[min], [max]] and snap to a multiple of [alignment]. */
    private fun alignToRange(value: Int, alignment: Int, min: Int, max: Int): Int {
        val clamped = value.coerceIn(min, max)
        if (alignment <= 1) return clamped
        var aligned = clamped / alignment * alignment
        if (aligned < min) aligned += alignment
        if (aligned > max) aligned = max / alignment * alignment
        return aligned
    }
}
