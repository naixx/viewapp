package com.github.naixx.viewapp.encoding.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import com.github.naixx.logger.LL
import java.io.File

/**
 * Helper class for testing YUV encoding and conversion issues
 */
class YuvDebugHelper(private val context: Context) {

    /**
     * Check if the color format is supported by the device
     */
    fun isSupportedColorFormat(colorFormat: Int): Boolean {
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoders = codecList.codecInfos.filter { it.isEncoder }

            for (encoder in encoders) {
                val types = encoder.supportedTypes
                if (types.contains(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    val capabilities = encoder.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                    val formats = capabilities.colorFormats
                    if (formats.contains(colorFormat)) {
                        return true
                    }
                }
            }
            return false
        } catch (e: Exception) {
            LL.e("Error checking supported color formats: ${e.message}")
            return false
        }
    }

    /**
     * Save a test image showing the YUV conversion
     * @return File path of the saved test image or null if failed
     */
    fun generateTestImage(sourceFrame: File): File? {
        try {
            // Load source bitmap
            val bitmap = BitmapFactory.decodeFile(sourceFrame.path) ?: return null

            // Convert to YUV
            val yuv = ColorConverter.convertBitmapToI420(bitmap)

            // Save the YUV data as a PNG for visual inspection
            val testFile = File(context.cacheDir, "yuv_test_${System.currentTimeMillis()}.png")
            ColorConverter.saveYuvAsPng(yuv, bitmap.width, bitmap.height, testFile)

            LL.d("Saved YUV test image to ${testFile.path}")
            return testFile
        } catch (e: Exception) {
            LL.e("Error generating test image: ${e.message}")
            return null
        }
    }

    /**
     * Gets information about the supported color formats
     */
    fun logSupportedColorFormats(): String {
        val result = StringBuilder()
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoders = codecList.codecInfos.filter { it.isEncoder }

            for (encoder in encoders) {
                val types = encoder.supportedTypes
                if (!types.contains(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    continue
                }

                val capabilities = encoder.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val formats = capabilities.colorFormats

                result.append("Encoder: ${encoder.name}, Supported formats: ")
                for (format in formats) {
                    val formatName = when (format) {
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> "YUV420Planar"
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> "YUV420PackedPlanar"
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> "YUV420SemiPlanar"
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> "YUV420PackedSemiPlanar"
                        MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888 -> "ARGB8888"
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBFlexible -> "RGBFlexible"
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Flexible -> "YUV422Flexible"
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Flexible -> "YUV444Flexible"
                        else -> "Unknown($format)"
                    }
                    result.append("$formatName, ")
                }
                result.append("\n")
            }

            LL.d("Supported color formats: $result")
        } catch (e: Exception) {
            LL.e("Error getting supported color formats: ${e.message}")
            result.append("Error: ${e.message}")
        }

        return result.toString()
    }
}
