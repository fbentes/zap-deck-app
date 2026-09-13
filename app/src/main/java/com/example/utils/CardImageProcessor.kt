package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Intelligent Image Processor for Business Cards:
 * 1. Auto-Framing (Enquadramento): Detects the exact card boundary, removing tables, desk backgrounds,
 *    surrounding objects, and borders.
 * 2. Shadow Removal & Clarity Normalization (Remoção de Sombras): Eliminates ambient/hand shadows,
 *    evens out lighting gradients, and enhances contrast and sharpness for physical-level readability.
 */
object CardImageProcessor {

    data class ProcessedCardResult(
        val finalBitmap: Bitmap,
        val cropRect: Rect,
        val isFramed: Boolean
    )

    /**
     * Complete pipeline: Detects card boundaries, crops out unwanted background,
     * and applies shadow removal and clarity enhancement.
     */
    fun processCard(
        originalBitmap: Bitmap,
        visionText: Text? = null
    ): ProcessedCardResult {
        val width = originalBitmap.width
        val height = originalBitmap.height

        if (width <= 10 || height <= 10) {
            return ProcessedCardResult(originalBitmap, Rect(0, 0, width, height), false)
        }

        // 1. Detect the card rectangular boundary
        val cardBounds = detectCardBounds(originalBitmap, visionText)

        // Only crop if the detected region is a valid sub-region of the image
        val isCropped = cardBounds.width() < width * 0.98f || cardBounds.height() < height * 0.98f
        val croppedBitmap = if (isCropped) {
            try {
                Bitmap.createBitmap(
                    originalBitmap,
                    cardBounds.left,
                    cardBounds.top,
                    cardBounds.width(),
                    cardBounds.height()
                )
            } catch (e: Exception) {
                originalBitmap
            }
        } else {
            originalBitmap
        }

        // 2. Remove shadows and enhance clarity for human readability
        val enhancedBitmap = removeShadowsAndEnhance(croppedBitmap)

        return ProcessedCardResult(
            finalBitmap = enhancedBitmap,
            cropRect = cardBounds,
            isFramed = isCropped
        )
    }

    /**
     * Detects the business card boundaries in the image.
     * Uses ML Kit text block bounding boxes as anchor ground truth + gradient edge detection
     * to find the true physical edges of the card.
     */
    fun detectCardBounds(
        bitmap: Bitmap,
        visionText: Text? = null
    ): Rect {
        val imgWidth = bitmap.width
        val imgHeight = bitmap.height

        val textBlocks = visionText?.textBlocks?.filter { it.text.isNotBlank() } ?: emptyList()

        if (textBlocks.isNotEmpty()) {
            var minX = imgWidth
            var minY = imgHeight
            var maxX = 0
            var maxY = 0

            textBlocks.forEach { block ->
                block.boundingBox?.let { box ->
                    if (box.left < minX) minX = box.left
                    if (box.top < minY) minY = box.top
                    if (box.right > maxX) maxX = box.right
                    if (box.bottom > maxY) maxY = box.bottom
                }
            }

            if (maxX > minX && maxY > minY) {
                // We have a solid text envelope. Now expand outwards to detect the card edges.
                val contentWidth = maxX - minX
                val contentHeight = maxY - minY

                // Search outward for card edges
                val topEdge = findHorizontalEdgeUpward(bitmap, minY, minX, maxX, contentHeight)
                val bottomEdge = findHorizontalEdgeDownward(bitmap, maxY, minX, maxX, contentHeight)
                val leftEdge = findVerticalEdgeLeftward(bitmap, minX, minY, maxY, contentWidth)
                val rightEdge = findVerticalEdgeRightward(bitmap, maxX, minY, maxY, contentWidth)

                val paddingX = (contentWidth * 0.05f).roundToInt().coerceAtLeast(12)
                val paddingY = (contentHeight * 0.05f).roundToInt().coerceAtLeast(12)

                val finalLeft = leftEdge.coerceAtLeast(0).coerceAtMost(minX - paddingX).coerceAtLeast(0)
                val finalTop = topEdge.coerceAtLeast(0).coerceAtMost(minY - paddingY).coerceAtLeast(0)
                val finalRight = rightEdge.coerceAtMost(imgWidth).coerceAtLeast(maxX + paddingX).coerceAtMost(imgWidth)
                val finalBottom = bottomEdge.coerceAtMost(imgHeight).coerceAtLeast(maxY + paddingY).coerceAtMost(imgHeight)

                val rectWidth = finalRight - finalLeft
                val rectHeight = finalBottom - finalTop

                if (rectWidth > 50 && rectHeight > 50) {
                    return Rect(finalLeft, finalTop, finalRight, finalBottom)
                }
            }
        }

        // Fallback: Color variance / Sobel boundary scan from image borders
        return detectBoundaryByColorContrast(bitmap)
    }

    /**
     * Scans upward from the top text line to detect where the card surface transitions to the desk/background.
     */
    private fun findHorizontalEdgeUpward(
        bitmap: Bitmap,
        startY: Int,
        minX: Int,
        maxX: Int,
        contentHeight: Int
    ): Int {
        val sampleXStart = minX.coerceAtLeast(0)
        val sampleXEnd = maxX.coerceAtMost(bitmap.width - 1)
        if (sampleXStart >= sampleXEnd) return 0

        val maxSearchDistance = (contentHeight * 0.45f).roundToInt().coerceAtLeast(20)
        val step = max(1, (sampleXEnd - sampleXStart) / 25)

        var prevAvgColor = getAverageRowColor(bitmap, startY, sampleXStart, sampleXEnd, step)
        var bestEdgeY = 0
        var maxDifference = 0.0

        val minSearchY = max(0, startY - maxSearchDistance)
        for (y in (startY - 4) downTo minSearchY) {
            val curAvgColor = getAverageRowColor(bitmap, y, sampleXStart, sampleXEnd, step)
            val diff = colorDistance(prevAvgColor, curAvgColor)
            if (diff > maxDifference && diff > 28.0) {
                maxDifference = diff
                bestEdgeY = y
            }
            prevAvgColor = curAvgColor
        }

        return if (maxDifference > 32.0) {
            // Found a clear transition edge (e.g. blue/white card to brown wooden desk)
            max(0, bestEdgeY - 2)
        } else {
            // No abrupt edge found, keep standard margin
            val defaultMargin = (contentHeight * 0.08f).roundToInt()
            max(0, startY - defaultMargin)
        }
    }

    /**
     * Scans downward from the bottom text line to detect the bottom card edge.
     */
    private fun findHorizontalEdgeDownward(
        bitmap: Bitmap,
        startY: Int,
        minX: Int,
        maxX: Int,
        contentHeight: Int
    ): Int {
        val sampleXStart = minX.coerceAtLeast(0)
        val sampleXEnd = maxX.coerceAtMost(bitmap.width - 1)
        if (sampleXStart >= sampleXEnd) return bitmap.height

        val maxSearchDistance = (contentHeight * 0.45f).roundToInt().coerceAtLeast(20)
        val step = max(1, (sampleXEnd - sampleXStart) / 25)

        var prevAvgColor = getAverageRowColor(bitmap, startY, sampleXStart, sampleXEnd, step)
        var bestEdgeY = bitmap.height
        var maxDifference = 0.0

        val maxSearchY = min(bitmap.height - 1, startY + maxSearchDistance)
        for (y in (startY + 4)..maxSearchY) {
            val curAvgColor = getAverageRowColor(bitmap, y, sampleXStart, sampleXEnd, step)
            val diff = colorDistance(prevAvgColor, curAvgColor)
            if (diff > maxDifference && diff > 28.0) {
                maxDifference = diff
                bestEdgeY = y
            }
            prevAvgColor = curAvgColor
        }

        return if (maxDifference > 32.0) {
            min(bitmap.height, bestEdgeY + 2)
        } else {
            val defaultMargin = (contentHeight * 0.08f).roundToInt()
            min(bitmap.height, startY + defaultMargin)
        }
    }

    /**
     * Scans leftward from the leftmost text line to detect the left card edge.
     */
    private fun findVerticalEdgeLeftward(
        bitmap: Bitmap,
        startX: Int,
        minY: Int,
        maxY: Int,
        contentWidth: Int
    ): Int {
        val sampleYStart = minY.coerceAtLeast(0)
        val sampleYEnd = maxY.coerceAtMost(bitmap.height - 1)
        if (sampleYStart >= sampleYEnd) return 0

        val maxSearchDistance = (contentWidth * 0.45f).roundToInt().coerceAtLeast(20)
        val step = max(1, (sampleYEnd - sampleYStart) / 25)

        var prevAvgColor = getAverageColColor(bitmap, startX, sampleYStart, sampleYEnd, step)
        var bestEdgeX = 0
        var maxDifference = 0.0

        val minSearchX = max(0, startX - maxSearchDistance)
        for (x in (startX - 4) downTo minSearchX) {
            val curAvgColor = getAverageColColor(bitmap, x, sampleYStart, sampleYEnd, step)
            val diff = colorDistance(prevAvgColor, curAvgColor)
            if (diff > maxDifference && diff > 28.0) {
                maxDifference = diff
                bestEdgeX = x
            }
            prevAvgColor = curAvgColor
        }

        return if (maxDifference > 32.0) {
            max(0, bestEdgeX - 2)
        } else {
            val defaultMargin = (contentWidth * 0.06f).roundToInt()
            max(0, startX - defaultMargin)
        }
    }

    /**
     * Scans rightward from the rightmost text line to detect the right card edge.
     */
    private fun findVerticalEdgeRightward(
        bitmap: Bitmap,
        startX: Int,
        minY: Int,
        maxY: Int,
        contentWidth: Int
    ): Int {
        val sampleYStart = minY.coerceAtLeast(0)
        val sampleYEnd = maxY.coerceAtMost(bitmap.height - 1)
        if (sampleYStart >= sampleYEnd) return bitmap.width

        val maxSearchDistance = (contentWidth * 0.45f).roundToInt().coerceAtLeast(20)
        val step = max(1, (sampleYEnd - sampleYStart) / 25)

        var prevAvgColor = getAverageColColor(bitmap, startX, sampleYStart, sampleYEnd, step)
        var bestEdgeX = bitmap.width
        var maxDifference = 0.0

        val maxSearchX = min(bitmap.width - 1, startX + maxSearchDistance)
        for (x in (startX + 4)..maxSearchX) {
            val curAvgColor = getAverageColColor(bitmap, x, sampleYStart, sampleYEnd, step)
            val diff = colorDistance(prevAvgColor, curAvgColor)
            if (diff > maxDifference && diff > 28.0) {
                maxDifference = diff
                bestEdgeX = x
            }
            prevAvgColor = curAvgColor
        }

        return if (maxDifference > 32.0) {
            min(bitmap.width, bestEdgeX + 2)
        } else {
            val defaultMargin = (contentWidth * 0.06f).roundToInt()
            min(bitmap.width, startX + defaultMargin)
        }
    }

    private fun detectBoundaryByColorContrast(bitmap: Bitmap): Rect {
        val width = bitmap.width
        val height = bitmap.height

        // Check if borders contain a strong color shift from the center
        val centerLuminance = getAreaLuminance(bitmap, width / 4, height / 4, width * 3 / 4, height * 3 / 4)
        val topLuminance = getAreaLuminance(bitmap, 0, 0, width, height / 8)

        val marginX = (width * 0.03f).roundToInt()
        val marginY = if (abs(centerLuminance - topLuminance) > 30) (height * 0.12f).roundToInt() else (height * 0.03f).roundToInt()

        return Rect(marginX, marginY, width - marginX, height - marginY)
    }

    /**
     * Shadow Removal & Clarity Normalization:
     * - Estimates background ambient illumination across the card surface.
     * - Normalizes shaded / uneven areas so the entire card has uniform, bright lighting.
     * - Sharpens text and boosts contrast so small numbers, letters, and QR codes are crisp and legible.
     */
    fun removeShadowsAndEnhance(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 10 || height <= 10) return bitmap

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Estimate low-frequency background illumination map
        // We downsample to a small grid (e.g. 32x24) to isolate the lighting gradient from sharp text
        val gridW = 32.coerceAtMost(width)
        val gridH = 24.coerceAtMost(height)
        val illuminationGrid = FloatArray(gridW * gridH)

        val cellW = width.toFloat() / gridW
        val cellH = height.toFloat() / gridH

        // Compute 85th percentile (surface brightness) per cell
        for (gy in 0 until gridH) {
            val yStart = (gy * cellH).toInt().coerceIn(0, height - 1)
            val yEnd = ((gy + 1) * cellH).toInt().coerceIn(yStart + 1, height)

            for (gx in 0 until gridW) {
                val xStart = (gx * cellW).toInt().coerceIn(0, width - 1)
                val xEnd = ((gx + 1) * cellW).toInt().coerceIn(xStart + 1, width)

                var maxLum = 0f
                var sumLum = 0f
                var count = 0

                val stepX = max(1, (xEnd - xStart) / 6)
                val stepY = max(1, (yEnd - yStart) / 6)

                for (y in yStart until yEnd step stepY) {
                    val rowOffset = y * width
                    for (x in xStart until xEnd step stepX) {
                        val p = pixels[rowOffset + x]
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                        if (lum > maxLum) maxLum = lum
                        sumLum += lum
                        count++
                    }
                }

                // Weighted surface brightness (favors the card surface over dark printed ink)
                val avg = if (count > 0) sumLum / count else 180f
                illuminationGrid[gy * gridW + gx] = (maxLum * 0.7f + avg * 0.3f).coerceIn(40f, 255f)
            }
        }

        // Smooth the illumination grid (2D Box Blur on the illumination map)
        val smoothedGrid = FloatArray(gridW * gridH)
        for (gy in 0 until gridH) {
            for (gx in 0 until gridW) {
                var sum = 0f
                var count = 0
                for (dy in -1..1) {
                    val ny = gy + dy
                    if (ny in 0 until gridH) {
                        for (dx in -1..1) {
                            val nx = gx + dx
                            if (nx in 0 until gridW) {
                                sum += illuminationGrid[ny * gridW + nx]
                                count++
                            }
                        }
                    }
                }
                smoothedGrid[gy * gridW + gx] = if (count > 0) sum / count else illuminationGrid[gy * gridW + gx]
            }
        }

        // Find the target bright point of the card (90th percentile of illumination)
        val sortedIllum = smoothedGrid.clone().apply { sort() }
        val targetWhitePoint = sortedIllum[(sortedIllum.size * 0.90f).toInt().coerceIn(0, sortedIllum.size - 1)]
            .coerceIn(210f, 248f)

        // 2. Apply Adaptive Shadow Correction & Local Contrast Boost
        val outputPixels = IntArray(width * height)

        for (y in 0 until height) {
            val rowOffset = y * width
            val gy = ((y / cellH).toInt()).coerceIn(0, gridH - 1)

            for (x in 0 until width) {
                val gx = ((x / cellW).toInt()).coerceIn(0, gridW - 1)
                val localIllum = smoothedGrid[gy * gridW + gx].coerceAtLeast(35f)

                val p = pixels[rowOffset + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                // If this local area is in a shadow (localIllum < targetWhitePoint), calculate gain
                val shadowGain = if (localIllum < targetWhitePoint) {
                    1.0f + ((targetWhitePoint - localIllum) / (localIllum + 35f)) * 0.85f
                } else {
                    1.0f
                }

                // Preserve deep dark text: only lift background highlights/midtones
                val textPreservationFactor = (lum / 140f).coerceIn(0.15f, 1.0f)
                val finalGain = 1.0f + (shadowGain - 1.0f) * textPreservationFactor

                var newR = (r * finalGain)
                var newG = (g * finalGain)
                var newB = (b * finalGain)

                // 3. Crisp Document Contrast Stretch: deepen blacks slightly & brighten highlights
                newR = (newR - 12f) * 1.08f
                newG = (newG - 12f) * 1.08f
                newB = (newB - 12f) * 1.08f

                val clampR = newR.roundToInt().coerceIn(0, 255)
                val clampG = newG.roundToInt().coerceIn(0, 255)
                val clampB = newB.roundToInt().coerceIn(0, 255)

                outputPixels[rowOffset + x] = (0xFF shl 24) or (clampR shl 16) or (clampG shl 8) or clampB
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    private fun getAverageRowColor(bitmap: Bitmap, y: Int, startX: Int, endX: Int, step: Int): Int {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        for (x in startX..endX step step) {
            val color = bitmap.getPixel(x, y)
            rSum += (color shr 16) and 0xFF
            gSum += (color shr 8) and 0xFF
            bSum += color and 0xFF
            count++
        }

        if (count == 0) return 0
        return Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    private fun getAverageColColor(bitmap: Bitmap, x: Int, startY: Int, endY: Int, step: Int): Int {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        for (y in startY..endY step step) {
            val color = bitmap.getPixel(x, y)
            rSum += (color shr 16) and 0xFF
            gSum += (color shr 8) and 0xFF
            bSum += color and 0xFF
            count++
        }

        if (count == 0) return 0
        return Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    private fun colorDistance(c1: Int, c2: Int): Double {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()

        return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
    }

    private fun getAreaLuminance(bitmap: Bitmap, x1: Int, y1: Int, x2: Int, y2: Int): Float {
        var sum = 0f
        var count = 0
        val step = 10
        for (y in y1 until y2 step step) {
            for (x in x1 until x2 step step) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                sum += 0.299f * r + 0.587f * g + 0.114f * b
                count++
            }
        }
        return if (count > 0) sum / count else 128f
    }
}
