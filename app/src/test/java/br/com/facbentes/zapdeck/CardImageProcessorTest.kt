package br.com.facbentes.zapdeck

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.facbentes.zapdeck.utils.CardImageProcessor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardImageProcessorTest {

    @Test
    fun testShadowRemovalAndEnhancement() {
        // Create a bitmap with uneven lighting (a dark shadow on one side)
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        for (y in 0 until 80) {
            for (x in 0 until 120) {
                // Gradient shadow: left side is darker (80), right side is brighter (220)
                val baseLum = 80 + (x * 140 / 120)
                bitmap.setPixel(x, y, Color.rgb(baseLum, baseLum, baseLum))
            }
        }

        val enhanced = CardImageProcessor.removeShadowsAndEnhance(bitmap)
        assertNotNull(enhanced)
        assertEquals(120, enhanced.width)
        assertEquals(80, enhanced.height)

        // Check that the left (previously shadowed) area has been significantly boosted
        val leftPixel = enhanced.getPixel(10, 40)
        val leftLum = Color.red(leftPixel)
        assertTrue("Shadow area should be brightened, got $leftLum", leftLum > 80)
    }

    @Test
    fun testDetectCardBoundsFallback() {
        // Create a card with a distinct center region and dark margins
        val bitmap = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888)
        for (y in 0 until 120) {
            for (x in 0 until 200) {
                if (x in 20..180 && y in 15..105) {
                    bitmap.setPixel(x, y, Color.rgb(240, 240, 240)) // Card body
                } else {
                    bitmap.setPixel(x, y, Color.rgb(40, 40, 40)) // Dark table / background
                }
            }
        }

        val bounds = CardImageProcessor.detectCardBounds(bitmap, null)
        assertNotNull(bounds)
        assertTrue("Bounds width should be valid", bounds.width() in 50..200)
        assertTrue("Bounds height should be valid", bounds.height() in 30..120)
    }

    @Test
    fun testProcessCardFullPipeline() {
        val bitmap = Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888)
        val result = CardImageProcessor.processCard(bitmap, null)
        assertNotNull(result.finalBitmap)
        assertNotNull(result.cropRect)
    }
}
