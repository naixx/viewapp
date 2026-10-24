package com.github.naixx.viewapp.utils

import coil3.map.Mapper
import coil3.request.Options
import github.naixx.network.Clip
import github.naixx.network.CoilMapper
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ClipMapper : Mapper<CoilMapper, ByteArray> {

    @OptIn(ExperimentalEncodingApi::class)
    override fun map(data: CoilMapper, options: Options): ByteArray? {
        return try {
            val cleanBase64 = data.imageBase64!!.trim()
            Base64.Default.decode(cleanBase64)
        } catch (e: Exception) {
            ByteArray(0)
            null
        }
    }
}
