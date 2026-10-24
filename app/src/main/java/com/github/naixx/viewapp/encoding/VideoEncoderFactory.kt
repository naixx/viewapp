package com.github.naixx.viewapp.encoding

import android.content.Context
import com.github.naixx.viewapp.encoding.android.MediaCodecEncoder
import github.naixx.core.FrameStore

/**
 * A factory for creating VideoEncoder instances based on platform capabilities
 */
interface VideoEncoderFactory {
    fun createEncoder(): VideoEncoder
}

/**
 * Creates encoders for the Android platform using MediaCodec
 */
class VideoEncoderFactoryImpl(private val context: Context, private val frames: FrameStore) : VideoEncoderFactory {
    override fun createEncoder(): VideoEncoder = MediaCodecEncoder(context, frames)
}
