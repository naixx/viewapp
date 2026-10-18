package com.github.naixx.viewapp.utils

import android.graphics.BitmapFactory
import java.io.File

/** The VIEW measures a 120 px wide copy of each frame; decode at least that wide. */
private const val MIN_DECODE_WIDTH = 120

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

/** Luma histogram of a frame JPEG, decoded downsampled; null if the file can't be decoded. */
fun lumaHistogram(file: File): List<Int>? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MIN_DECODE_WIDTH) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    bitmap.recycle()
    return lumaHistogram(pixels).asList()
}
