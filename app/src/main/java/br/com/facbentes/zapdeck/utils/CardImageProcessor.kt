package br.com.facbentes.zapdeck.utils

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
                // Ensure text box is bounded within image
                minX = minX.coerceIn(0, imgWidth - 1)
                minY = minY.coerceIn(0, imgHeight - 1)
                maxX = maxX.coerceIn(minX + 1, imgWidth)
                maxY = maxY.coerceIn(minY + 1, imgHeight)

                // 1. Determine baseline card surface color (sample between minX and maxX, minY and maxY)
                val cardSurface = sampleCardSurfaceColor(bitmap, minX, minY, maxX, maxY)

                // 2. Scan outward in each of the 4 directions to find the card-to-table transition:
                val topEdge = findHorizontalEdge(bitmap, startY = minY, direction = -1, minX = minX, maxX = maxX, cardColor = cardSurface)
                val bottomEdge = findHorizontalEdge(bitmap, startY = maxY, direction = 1, minX = minX, maxX = maxX, cardColor = cardSurface)
                val leftEdge = findVerticalEdge(bitmap, startX = minX, direction = -1, minY = minY, maxY = maxY, cardColor = cardSurface)
                val rightEdge = findVerticalEdge(bitmap, startX = maxX, direction = 1, minY = minY, maxY = maxY, cardColor = cardSurface)

                // Small buffer (1-2% of card dimension, ~8-12px) to preserve physical bevel without including desk
                val bufX = max(6, ((maxX - minX) * 0.02f).roundToInt())
                val bufY = max(6, ((maxY - minY) * 0.02f).roundToInt())

                val finalLeft = max(0, leftEdge - bufX)
                val finalTop = max(0, topEdge - bufY)
                val finalRight = min(imgWidth, rightEdge + bufX)
                val finalBottom = min(imgHeight, bottomEdge + bufY)

                val rectWidth = finalRight - finalLeft
                val rectHeight = finalBottom - finalTop

                // Ensure it encompasses at least the text
                if (rectWidth > 50 && rectHeight > 50) {
                    val safeLeft = min(finalLeft, max(0, minX - 8))
                    val safeTop = min(finalTop, max(0, minY - 8))
                    val safeRight = max(finalRight, min(imgWidth, maxX + 8))
                    val safeBottom = max(finalBottom, min(imgHeight, maxY + 8))
                    return Rect(safeLeft, safeTop, safeRight, safeBottom)
                }
            }
        }

        // Fallback: Color contrast boundary scan from image borders
        return detectBoundaryByColorContrast(bitmap)
    }

    /**
     * Scans horizontally outward (up or down) from text block to detect where the card meets the desk.
     */
    private fun findHorizontalEdge(
        bitmap: Bitmap,
        startY: Int,
        direction: Int, // -1 for upward, +1 for downward
        minX: Int,
        maxX: Int,
        cardColor: Int
    ): Int {
        val width = bitmap.width
        val height = bitmap.height
        val sampleX1 = (minX + (maxX - minX) * 0.10f).toInt().coerceIn(0, width - 1)
        val sampleX2 = (maxX - (maxX - minX) * 0.10f).toInt().coerceIn(sampleX1 + 1, width)
        val stepX = max(1, (sampleX2 - sampleX1) / 30)

        val cardLum = getLuminance(cardColor)
        val limitY = if (direction < 0) 0 else height - 1

        var bestEdgeY = if (direction < 0) 0 else height - 1
        var foundTransition = false

        var y = startY
        while (if (direction < 0) y >= limitY else y <= limitY) {
            val curColor = getAverageRowColor(bitmap, y, sampleX1, sampleX2, stepX)
            val curLum = getLuminance(curColor)
            val distFromCard = colorDistance(cardColor, curColor)

            // Look-ahead gradient comparison (4 rows ahead vs 4 rows behind)
            val yAhead = (y + direction * 4).coerceIn(0, height - 1)
            val yBehind = (y - direction * 4).coerceIn(0, height - 1)
            val colorAhead = getAverageRowColor(bitmap, yAhead, sampleX1, sampleX2, stepX)
            val colorBehind = getAverageRowColor(bitmap, yBehind, sampleX1, sampleX2, stepX)
            val gradient = colorDistance(colorAhead, colorBehind)

            // Transition from card surface to desk:
            if ((distFromCard > 38.0 || abs(curLum - cardLum) > 32f) && gradient > 20.0) {
                bestEdgeY = y
                foundTransition = true
                break
            } else if (distFromCard > 60.0 || abs(curLum - cardLum) > 50f) {
                bestEdgeY = y
                foundTransition = true
                break
            }

            y += direction
        }

        return if (foundTransition) {
            bestEdgeY
        } else {
            if (direction < 0) {
                max(0, startY - (height * 0.12f).roundToInt())
            } else {
                min(height, startY + (height * 0.12f).roundToInt())
            }
        }
    }

    /**
     * Scans vertically outward (left or right) from text block to detect where the card meets the desk.
     */
    private fun findVerticalEdge(
        bitmap: Bitmap,
        startX: Int,
        direction: Int, // -1 for leftward, +1 for rightward
        minY: Int,
        maxY: Int,
        cardColor: Int
    ): Int {
        val width = bitmap.width
        val height = bitmap.height
        val sampleY1 = (minY + (maxY - minY) * 0.10f).toInt().coerceIn(0, height - 1)
        val sampleY2 = (maxY - (maxY - minY) * 0.10f).toInt().coerceIn(sampleY1 + 1, height)
        val stepY = max(1, (sampleY2 - sampleY1) / 30)

        val cardLum = getLuminance(cardColor)
        val limitX = if (direction < 0) 0 else width - 1

        var bestEdgeX = if (direction < 0) 0 else width - 1
        var foundTransition = false

        var x = startX
        while (if (direction < 0) x >= limitX else x <= limitX) {
            val curColor = getAverageColColor(bitmap, x, sampleY1, sampleY2, stepY)
            val curLum = getLuminance(curColor)
            val distFromCard = colorDistance(cardColor, curColor)

            val xAhead = (x + direction * 4).coerceIn(0, width - 1)
            val xBehind = (x - direction * 4).coerceIn(0, width - 1)
            val colorAhead = getAverageColColor(bitmap, xAhead, sampleY1, sampleY2, stepY)
            val colorBehind = getAverageColColor(bitmap, xBehind, sampleY1, sampleY2, stepY)
            val gradient = colorDistance(colorAhead, colorBehind)

            if ((distFromCard > 38.0 || abs(curLum - cardLum) > 32f) && gradient > 20.0) {
                bestEdgeX = x
                foundTransition = true
                break
            } else if (distFromCard > 60.0 || abs(curLum - cardLum) > 50f) {
                bestEdgeX = x
                foundTransition = true
                break
            }

            x += direction
        }

        return if (foundTransition) {
            bestEdgeX
        } else {
            if (direction < 0) {
                max(0, startX - (width * 0.10f).roundToInt())
            } else {
                min(width, startX + (width * 0.10f).roundToInt())
            }
        }
    }

    private fun sampleCardSurfaceColor(
        bitmap: Bitmap,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): Int {
        val width = bitmap.width
        val height = bitmap.height
        val sampleX1 = (minX + (maxX - minX) * 0.20f).toInt().coerceIn(0, width - 1)
        val sampleX2 = (maxX - (maxX - minX) * 0.20f).toInt().coerceIn(sampleX1 + 1, width)
        val sampleY1 = (minY + (maxY - minY) * 0.20f).toInt().coerceIn(0, height - 1)
        val sampleY2 = (maxY - (maxY - minY) * 0.20f).toInt().coerceIn(sampleY1 + 1, height)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        val stepX = max(1, (sampleX2 - sampleX1) / 15)
        val stepY = max(1, (sampleY2 - sampleY1) / 15)

        for (y in sampleY1 until sampleY2 step stepY) {
            for (x in sampleX1 until sampleX2 step stepX) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 130f) {
                    rSum += r
                    gSum += g
                    bSum += b
                    count++
                }
            }
        }

        return if (count > 0) {
            Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
        } else {
            Color.rgb(240, 240, 240)
        }
    }

    private fun getLuminance(color: Int): Float {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun detectBoundaryByColorContrast(bitmap: Bitmap): Rect {
        val width = bitmap.width
        val height = bitmap.height

        val centerColor = getAverageRowColor(bitmap, height / 2, width / 4, width * 3 / 4, 10)
        val centerLum = getLuminance(centerColor)

        // Scan top inward (from 0 down to height/2)
        var top = 0
        for (y in 0 until height / 2 step 4) {
            val rowColor = getAverageRowColor(bitmap, y, width / 4, width * 3 / 4, 10)
            val rowLum = getLuminance(rowColor)
            if (colorDistance(rowColor, centerColor) < 35.0 && abs(rowLum - centerLum) < 30f) {
                top = max(0, y - 4)
                break
            }
        }

        // Scan bottom inward (from height - 1 down to height/2)
        var bottom = height
        for (y in (height - 1) downTo height / 2 step 4) {
            val rowColor = getAverageRowColor(bitmap, y, width / 4, width * 3 / 4, 10)
            val rowLum = getLuminance(rowColor)
            if (colorDistance(rowColor, centerColor) < 35.0 && abs(rowLum - centerLum) < 30f) {
                bottom = min(height, y + 4)
                break
            }
        }

        // Scan left inward (from 0 to width/2)
        var left = 0
        for (x in 0 until width / 2 step 4) {
            val colColor = getAverageColColor(bitmap, x, height / 4, height * 3 / 4, 10)
            val colLum = getLuminance(colColor)
            if (colorDistance(colColor, centerColor) < 35.0 && abs(colLum - centerLum) < 30f) {
                left = max(0, x - 4)
                break
            }
        }

        // Scan right inward (from width - 1 down to width/2)
        var right = width
        for (x in (width - 1) downTo width / 2 step 4) {
            val colColor = getAverageColColor(bitmap, x, height / 4, height * 3 / 4, 10)
            val colLum = getLuminance(colColor)
            if (colorDistance(colColor, centerColor) < 35.0 && abs(colLum - centerLum) < 30f) {
                right = min(width, x + 4)
                break
            }
        }

        if (right - left > 50 && bottom - top > 50) {
            return Rect(left, top, right, bottom)
        }
        return Rect(0, 0, width, height)
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
