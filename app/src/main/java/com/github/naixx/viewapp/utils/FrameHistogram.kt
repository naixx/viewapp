package com.github.naixx.viewapp.utils

import android.graphics.BitmapFactory
import github.naixx.core.lumaHistogram
import java.io.File

/** The VIEW measures a 120 px wide copy of each frame; decode at least that wide. */
private const val MIN_DECODE_WIDTH = 120

/** Luma histogram of a frame JPEG, decoded downsampled; null if the file can't be decoded. The binning is :core's. */
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
