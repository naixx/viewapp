package com.github.naixx.viewapp.utils

import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Luma histogram of a frame JPEG (the device's per-frame thumbnail, ~160 px wide), or null if it can't be decoded.
 * Decoded through Compose's `ImageBitmap` on every platform.
 */
fun lumaHistogram(jpeg: ByteArray): List<Int>? {
    val image = runCatching { jpeg.decodeToImageBitmap() }.getOrNull() ?: return null
    val pixels = IntArray(image.width * image.height)
    image.readPixels(pixels)
    return lumaHistogram(pixels).asList()
}

/**
 * 256-bin luma histogram of ARGB pixels, computed the way the VIEW does it (`jpeg-lum`, `lib.cc`:
 * BT.601 weights in float, truncated), so a live frame gets the same histogram the device sent for it.
 */
fun lumaHistogram(pixels: IntArray): IntArray {
    val bins = IntArray(256)
    for (p in pixels) {
        val r = (p shr 16) and 0xff
        val g = (p shr 8) and 0xff
        val b = p and 0xff
        bins[(r * 0.299f + g * 0.587f + b * 0.114f).toInt().coerceAtMost(255)]++
    }
    return bins
}
