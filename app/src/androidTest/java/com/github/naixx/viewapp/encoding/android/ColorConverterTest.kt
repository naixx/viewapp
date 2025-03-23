package com.github.naixx.viewapp.encoding.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ColorConverterTest {

    private lateinit var testBitmap: Bitmap
    private val width = 16
    private val height = 16

    @Before
    fun setUp() {
        // Create a test bitmap with a simple pattern
        testBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(testBitmap)
        val paint = Paint()

        // Fill with white background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw a red rectangle in the top-left quarter
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, width/2f, height/2f, paint)

        // Draw a green rectangle in the top-right quarter
        paint.color = Color.GREEN
        canvas.drawRect(width/2f, 0f, width.toFloat(), height/2f, paint)

        // Draw a blue rectangle in the bottom-left quarter
        paint.color = Color.BLUE
        canvas.drawRect(0f, height/2f, width/2f, height.toFloat(), paint)

        // Draw a black rectangle in the bottom-right quarter
        paint.color = Color.BLACK
        canvas.drawRect(width/2f, height/2f, width.toFloat(), height.toFloat(), paint)
    }

    @Test
    fun testConvertBitmapToI420() {
        // Convert bitmap to YUV
        val yuvData = ColorConverter.convertBitmapToI420(testBitmap)

        // Check overall buffer size
        val expectedSize = width * height + ((width/2) * (height/2)) * 2 // Y + U + V
        assertEquals(expectedSize, yuvData.size)

        // Test Y channel values for known colors
        val yOffset = 0

        // Check RED Y value (top-left)
        val redY = yuvData[yOffset + 0].toInt() and 0xFF
        assertEquals("Red Y value", 76.0, redY.toDouble(), 5.0) // Y for RED ≈ 76

        // Check GREEN Y value (top-right)
        val greenY = yuvData[yOffset + (width/2)].toInt() and 0xFF
        assertEquals("Green Y value", 149.0, greenY.toDouble(), 5.0) // Y for GREEN ≈ 149

        // Check BLUE Y value (bottom-left)
        val blueY = yuvData[yOffset + (width * (height/2))].toInt() and 0xFF
        assertEquals("Blue Y value", 29.0, blueY.toDouble(), 5.0) // Y for BLUE ≈ 29

        // Check BLACK Y value (bottom-right)
        val blackY = yuvData[yOffset + (width * (height/2)) + (width/2)].toInt() and 0xFF
        assertEquals("Black Y value", 0.0, blackY.toDouble(), 5.0) // Y for BLACK ≈ 0

        // Check U and V values
        val uOffset = width * height
        val vOffset = uOffset + ((width/2) * (height/2))

        // RED: U ≈ 128 (neutral), V ≈ 255 (high)
        val redU = yuvData[uOffset + 0].toInt() and 0xFF
        val redV = yuvData[vOffset + 0].toInt() and 0xFF
        assertEquals("Red U value", 128.0, redU.toDouble(), 30.0)
        assertTrue(redV > 200)

        // BLUE: U ≈ 255 (high), V ≈ 128 (neutral)
        val blueU = yuvData[uOffset + (width/4)].toInt() and 0xFF
        val blueV = yuvData[vOffset + (width/4)].toInt() and 0xFF
        assertTrue(blueU > 200)
        assertEquals("Blue V value", 128.0, blueV.toDouble(), 30.0)
    }

    @Test
    fun testSaveAndReadYuvImage() {
        // Convert bitmap to YUV
        val yuvData = ColorConverter.convertBitmapToI420(testBitmap)

        // Save as PNG for visual inspection
        val context = InstrumentationRegistry.getInstrumentation().context
        val outputFile = File(context.cacheDir, "test_yuv.png")

        ColorConverter.saveYuvAsPng(yuvData, width, height, outputFile)

        // Verify file was created
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
    }
}
