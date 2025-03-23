package com.github.naixx.viewapp.encoding.android

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper class for color conversion and testing
 */
object ColorConverter {

    /**
     * Convert RGB bitmap to I420 (YUV420) format
     * This format is widely supported by video encoders
     */
    @JvmStatic
    fun convertBitmapToI420(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val frameSize = width * height

        // Allocate buffer - Y + U + V
        // Y = width * height
        // U = (width/2) * (height/2)
        // V = (width/2) * (height/2)
        val yuvBuffer = ByteArray(frameSize + (frameSize / 2))

        // Read all pixels
        val argbPixels = IntArray(frameSize)
        bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)

        // Fill Y channel
        var yIndex = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val index = j * width + i
                val argb = argbPixels[index]

                // Extract RGB values
                val r = Color.red(argb)
                val g = Color.green(argb)
                val b = Color.blue(argb)

                // RGB to Y conversion
                val y = clamp((0.299 * r + 0.587 * g + 0.114 * b).toInt())
                yuvBuffer[yIndex++] = y.toByte()
            }
        }

        // Fill U and V channels
        var uIndex = frameSize
        var vIndex = frameSize + (frameSize / 4)

        // For each 2x2 block
        for (j in 0 until height step 2) {
            for (i in 0 until width step 2) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0

                // Sample 4 pixels
                for (jj in 0 until 2) {
                    for (ii in 0 until 2) {
                        val x = i + ii
                        val y = j + jj

                        // Skip pixels outside the image
                        if (x >= width || y >= height) continue

                        val index = y * width + x
                        val argb = argbPixels[index]

                        sumR += Color.red(argb)
                        sumG += Color.green(argb)
                        sumB += Color.blue(argb)
                        count++
                    }
                }

                // Calculate average RGB
                val avgR = if (count > 0) sumR / count else 0
                val avgG = if (count > 0) sumG / count else 0
                val avgB = if (count > 0) sumB / count else 0

                // RGB to UV conversion
                val u = clamp((-0.14713 * avgR - 0.28886 * avgG + 0.436 * avgB + 128).toInt())
                val v = clamp((0.615 * avgR - 0.51499 * avgG - 0.10001 * avgB + 128).toInt())

                // Store U and V
                yuvBuffer[uIndex++] = u.toByte()
                yuvBuffer[vIndex++] = v.toByte()
            }
        }

        return yuvBuffer
    }

    /**
     * Clamp value to 0-255 range
     */
    @JvmStatic
    private fun clamp(value: Int): Int {
        return value.coerceIn(0, 255)
    }

    /**
     * Save YUV buffer as PNG for testing
     */
    @JvmStatic
    fun saveYuvAsPng(yuvBuffer: ByteArray, width: Int, height: Int, outputFile: File) {
        val bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888)
        val frameSize = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIndex = j * width + i
                val y = yuvBuffer[yIndex].toInt() and 0xFF

                // Find corresponding U and V values
                val uvBlockX = i / 2
                val uvBlockY = j / 2
                val uvIndex = (uvBlockY * (width / 2)) + uvBlockX

                // U and V are after Y in the buffer
                val uIndex = frameSize + uvIndex
                val vIndex = frameSize + (frameSize / 4) + uvIndex

                val u = if (uIndex < yuvBuffer.size) yuvBuffer[uIndex].toInt() and 0xFF else 128
                val v = if (vIndex < yuvBuffer.size) yuvBuffer[vIndex].toInt() and 0xFF else 128

                // Convert YUV to RGB
                val rgb = yuvToRgb(y, u, v)
                bitmap.setPixel(i, j, rgb)
            }
        }

        // Save bitmap to file
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /**
     * Convert YUV values to RGB color
     */
    @JvmStatic
    private fun yuvToRgb(y: Int, u: Int, v: Int): Int {
        // YUV to RGB conversion
        val yValue = y
        val uValue = u - 128
        val vValue = v - 128

        var r: Int = (yValue + (1.370705 * vValue)).toInt()
        var g: Int = (yValue - (0.698001 * vValue) - (0.337633 * uValue)).toInt()
        var b: Int = (yValue + (1.732446 * uValue)).toInt()

        r = clamp(r)
        g = clamp(g)
        b = clamp(b)

        return Color.rgb(r, g, b)
    }
}
