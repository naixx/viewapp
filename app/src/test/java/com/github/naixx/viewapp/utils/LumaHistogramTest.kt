package com.github.naixx.viewapp.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class LumaHistogramTest {

    private fun rgb(r: Int, g: Int, b: Int, a: Int = 0xff) = (a shl 24) or (r shl 16) or (g shl 8) or b

    // Bins the VIEW's jpeg-lum gives: truncated float 0.299 R + 0.587 G + 0.114 B.
    @Test
    fun pixelsLandInTheVIEWsBins() {
        val bins = lumaHistogram(intArrayOf(
            rgb(0, 0, 0), rgb(255, 255, 255), rgb(255, 0, 0), rgb(0, 255, 0), rgb(0, 0, 255), rgb(128, 128, 128)
        ))
        assertEquals(mapOf(0 to 1, 255 to 1, 76 to 1, 149 to 1, 29 to 1, 128 to 1), bins.withIndex().filter { it.value > 0 }.associate { it.index to it.value })
    }

    @Test
    fun countsEveryPixelAndIgnoresAlpha() {
        val bins = lumaHistogram(IntArray(100) { rgb(it, it, it, a = it) })
        assertEquals(256, bins.size)
        assertEquals(100, bins.sum())
        assertEquals(1, bins[99])
    }
}
