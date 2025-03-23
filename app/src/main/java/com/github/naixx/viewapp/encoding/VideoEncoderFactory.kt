package com.github.naixx.viewapp.encoding

import android.content.Context
import java.io.File

/**
 * A factory for creating VideoEncoder instances based on platform capabilities
 */
interface VideoEncoderFactory {
    fun createEncoder(): VideoEncoder
}

/**
 * Creates encoders for the Android platform using MediaCodec
 */
class VideoEncoderFactoryImpl(private val context: Context) : VideoEncoderFactory {
    override fun createEncoder(): VideoEncoder {
        // We'll use the MediaCodecEncoder from the android package
        return com.github.naixx.viewapp.encoding.android.MediaCodecEncoder(context)
    }
}
