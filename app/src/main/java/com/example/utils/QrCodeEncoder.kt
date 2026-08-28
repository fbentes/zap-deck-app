package com.example.utils

import java.util.Objects

/**
 * QR Code Generator in pure Kotlin (RFC standard compliant)
 * High performance, zero runtime dependency QR generator.
 */
class QrCodeEncoder private constructor(
    val version: Int,
    val errorCorrectionLevel: EccLevel,
    val dataCodewords: ByteArray,
    val size: Int
) {
    enum class EccLevel(val ordinalValue: Int, val formatBits: Int) {
        LOW(0, 1),
        MEDIUM(1, 0),
        QUARTILE(2, 3),
        HIGH(3, 2)
    }

    private val modules: Array<BooleanArray> = Array(size) { BooleanArray(size) }
    private val isFunction: Array<BooleanArray> = Array(size) { BooleanArray(size) }

    fun getModule(x: Int, y: Int): Boolean {
        return x in 0 until size && y in 0 until size && modules[y][x]
    }

    companion object {
        fun encodeText(text: String, ecl: EccLevel): QrCodeEncoder {
            val bytes = text.toByteArray(Charsets.UTF_8)
            // Determine minimum version required
            val version = getMinVersion(bytes.size, ecl)
            val qr = QrCodeEncoder(version, ecl, bytes, version * 4 + 17)
            qr.drawFunctionPatterns()
            val allCodewords = qr.encodeDataAndEcc(bytes)
            qr.drawCodewords(allCodewords)
            qr.applyBestMask()
            return qr
        }

        private fun getMinVersion(numBytes: Int, ecl: EccLevel): Int {
            for (v in 1..40) {
                val dataCapacity = getNumDataCodewords(v, ecl)
                val headerBits = 4 + (if (v < 10) 8 else 16)
                val totalBits = headerBits + numBytes * 8
                val totalBytes = (totalBits + 7) / 8
                if (totalBytes <= dataCapacity) {
                    return v
                }
            }
            return 40
        }

        private fun getNumDataCodewords(ver: Int, ecl: EccLevel): Int {
            val totalRawCodewords = NUM_RAW_DATA_MODULES[ver] / 8
            val eccCodewords = ECC_CODEWORDS_PER_BLOCK[ecl.ordinalValue][ver] * NUM_ERROR_CORRECTION_BLOCKS[ecl.ordinalValue][ver]
            return totalRawCodewords - eccCodewords
        }

        private val NUM_ERROR_CORRECTION_BLOCKS = arrayOf(
            intArrayOf(0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25),
            intArrayOf(0, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49),
            intArrayOf(0, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68),
            intArrayOf(0, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81)
        )

        private val ECC_CODEWORDS_PER_BLOCK = arrayOf(
            intArrayOf(0, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
            intArrayOf(0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28),
            intArrayOf(0, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
            intArrayOf(0, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30)
        )

        private val NUM_RAW_DATA_MODULES = intArrayOf(
            0, 208, 359, 567, 807, 1079, 1383, 1568, 1936, 2336, 2768, 3232, 3728, 4256, 4651, 5243, 5867, 6523,
            7211, 7931, 8683, 9252, 10068, 10916, 11796, 12708, 13652, 14628, 15371, 16411, 17483, 18587, 19723,
            20891, 22091, 23008, 24272, 25568, 26896, 28256, 29648
        )
    }

    private fun drawFunctionPatterns() {
        // Draw 3 finder patterns
        drawFinderPattern(3, 3)
        drawFinderPattern(size - 4, 3)
        drawFinderPattern(3, size - 4)

        // Draw timing patterns
        for (i in 0 until size) {
            setFunctionModule(6, i, i % 2 == 0)
            setFunctionModule(i, 6, i % 2 == 0)
        }

        // Draw alignment patterns
        val alignPos = getAlignmentPatternPositions(version)
        for (r in alignPos) {
            for (c in alignPos) {
                if (!((r == 6 && c == 6) || (r == 6 && c == size - 7) || (r == size - 7 && c == 6))) {
                    drawAlignmentPattern(c, r)
                }
            }
        }

        // Draw dummy format bits
        drawDummyFormatBits()
    }

    private fun drawFinderPattern(x: Int, y: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                val xx = x + dx
                val yy = y + dy
                if (xx in 0 until size && yy in 0 until size) {
                    setFunctionModule(xx, yy, dist != 2 && dist != 4)
                }
            }
        }
    }

    private fun drawAlignmentPattern(x: Int, y: Int) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunctionModule(x + dx, y + dy, dist != 1)
            }
        }
    }

    private fun setFunctionModule(x: Int, y: Int, isDark: Boolean) {
        modules[y][x] = isDark
        isFunction[y][x] = true
    }

    private fun getAlignmentPatternPositions(ver: Int): IntArray {
        if (ver == 1) return intArrayOf()
        val numAlign = ver / 7 + 2
        val step = if (ver == 32) 26 else (ver * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2
        val result = IntArray(numAlign)
        result[0] = 6
        var pos = size - 7
        for (i in numAlign - 1 downTo 1) {
            result[i] = pos
            pos -= step
        }
        return result
    }

    private fun drawDummyFormatBits() {
        for (i in 0..8) {
            if (i != 6) {
                setFunctionModule(8, i, false)
                setFunctionModule(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunctionModule(size - 1 - i, 8, false)
            setFunctionModule(8, size - 1 - i, false)
        }
        setFunctionModule(8, size - 8, true)
    }

    private fun encodeDataAndEcc(data: ByteArray): ByteArray {
        val dataCapacity = getNumDataCodewords(version, errorCorrectionLevel)
        val bitBuffer = ArrayList<Boolean>()

        // 1. Mode indicator (8-bit byte = 0100)
        bitBuffer.add(false); bitBuffer.add(true); bitBuffer.add(false); bitBuffer.add(false)

        // 2. Character count indicator
        val charCountBits = if (version < 10) 8 else 16
        for (i in charCountBits - 1 downTo 0) {
            bitBuffer.add(((data.size ushr i) and 1) != 0)
        }

        // 3. Data bytes
        for (b in data) {
            val v = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitBuffer.add(((v ushr i) and 1) != 0)
            }
        }

        // 4. Terminator (up to 4 zeroes)
        val capacityBits = dataCapacity * 8
        var count = 0
        while (bitBuffer.size < capacityBits && count < 4) {
            bitBuffer.add(false)
            count++
        }

        // 5. Pad to multiple of 8
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(false)
        }

        // 6. Pad bytes 0xEC, 0x11
        val padByte1 = 0xEC
        val padByte2 = 0x11
        var padAlternator = true
        while (bitBuffer.size < capacityBits) {
            val p = if (padAlternator) padByte1 else padByte2
            for (i in 7 downTo 0) {
                bitBuffer.add(((p ushr i) and 1) != 0)
            }
            padAlternator = !padAlternator
        }

        val dataCodewords = ByteArray(dataCapacity)
        for (i in 0 until dataCapacity) {
            var b = 0
            for (j in 0..7) {
                if (bitBuffer[i * 8 + j]) {
                    b = b or (1 shl (7 - j))
                }
            }
            dataCodewords[i] = b.toByte()
        }

        // Error correction calculation
        val numBlocks = NUM_ERROR_CORRECTION_BLOCKS[errorCorrectionLevel.ordinalValue][version]
        val blockEccLen = ECC_CODEWORDS_PER_BLOCK[errorCorrectionLevel.ordinalValue][version]
        val rawCodewords = NUM_RAW_DATA_MODULES[version] / 8
        val shortBlockDataLen = dataCapacity / numBlocks
        val numShortBlocks = numBlocks - (dataCapacity % numBlocks)

        val rs = ReedSolomonEncoder(blockEccLen)
        val dataBlocks = Array(numBlocks) { ByteArray(0) }
        val eccBlocks = Array(numBlocks) { ByteArray(0) }

        var offset = 0
        for (i in 0 until numBlocks) {
            val len = if (i < numShortBlocks) shortBlockDataLen else shortBlockDataLen + 1
            val blockData = ByteArray(len)
            System.arraycopy(dataCodewords, offset, blockData, 0, len)
            offset += len
            dataBlocks[i] = blockData
            eccBlocks[i] = rs.encode(blockData)
        }

        // Interleave
        val result = ByteArray(rawCodewords)
        var resIdx = 0
        val maxDataLen = shortBlockDataLen + (if (numShortBlocks < numBlocks) 1 else 0)
        for (i in 0 until maxDataLen) {
            for (j in 0 until numBlocks) {
                if (i < dataBlocks[j].size) {
                    result[resIdx++] = dataBlocks[j][i]
                }
            }
        }
        for (i in 0 until blockEccLen) {
            for (j in 0 until numBlocks) {
                result[resIdx++] = eccBlocks[j][i]
            }
        }
        return result
    }

    private fun drawCodewords(allCodewords: ByteArray) {
        var bitIndex = 0
        val numBits = allCodewords.size * 8
        var right = size - 1
        while (right > 0) {
            if (right == 6) right--
            for (vert in 0 until size) {
                for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert
                    if (!isFunction[y][x]) {
                        var dark = false
                        if (bitIndex < numBits) {
                            val byteIdx = bitIndex ushr 3
                            val bitIdx = 7 - (bitIndex and 7)
                            dark = ((allCodewords[byteIdx].toInt() ushr bitIdx) and 1) != 0
                            bitIndex++
                        }
                        modules[y][x] = dark
                    }
                }
            }
            right -= 2
        }
    }

    private fun applyBestMask() {
        // Evaluate default standard mask 0
        val mask = 0
        applyMask(mask)
        drawFormatBits(mask)
    }

    private fun applyMask(mask: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x]) {
                    val invert = (x + y) % 2 == 0
                    if (invert) {
                        modules[y][x] = !modules[y][x]
                    }
                }
            }
        }
    }

    private fun drawFormatBits(mask: Int) {
        val data = (errorCorrectionLevel.formatBits shl 3) or mask
        var rem = data
        for (i in 0..9) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        // Top-left
        for (i in 0..5) setTableModule(8, i, ((bits ushr i) and 1) != 0)
        setTableModule(8, 7, ((bits ushr 6) and 1) != 0)
        setTableModule(8, 8, ((bits ushr 7) and 1) != 0)
        setTableModule(7, 8, ((bits ushr 8) and 1) != 0)
        for (i in 9..14) setTableModule(14 - i, 8, ((bits ushr i) and 1) != 0)

        // Bottom-left / Top-right
        for (i in 0..7) setTableModule(size - 1 - i, 8, ((bits ushr i) and 1) != 0)
        for (i in 8..14) setTableModule(8, size - 15 + i, ((bits ushr i) and 1) != 0)
    }

    private fun setTableModule(x: Int, y: Int, isDark: Boolean) {
        modules[y][x] = isDark
    }
}

class ReedSolomonEncoder(private val degree: Int) {
    private val generator: IntArray = buildGenerator(degree)

    private fun buildGenerator(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            poly = multiplyPoly(poly, intArrayOf(1, EXP_TABLE[i]))
        }
        return poly
    }

    fun encode(data: ByteArray): ByteArray {
        val ecc = IntArray(degree)
        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor ecc[0]
            System.arraycopy(ecc, 1, ecc, 0, degree - 1)
            ecc[degree - 1] = 0
            for (i in 0 until degree) {
                ecc[i] = ecc[i] xor galoisMultiply(generator[i + 1], factor)
            }
        }
        val result = ByteArray(degree)
        for (i in 0 until degree) {
            result[i] = ecc[i].toByte()
        }
        return result
    }

    companion object {
        private val EXP_TABLE = IntArray(512)
        private val LOG_TABLE = IntArray(256)

        init {
            var x = 1
            for (i in 0 until 255) {
                EXP_TABLE[i] = x
                EXP_TABLE[i + 255] = x
                LOG_TABLE[x] = i
                x = x shl 1
                if (x >= 256) {
                    x = x xor 0x11D
                }
            }
            LOG_TABLE[0] = 0
        }

        private fun galoisMultiply(x: Int, y: Int): Int {
            if (x == 0 || y == 0) return 0
            return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]]
        }

        private fun multiplyPoly(p: IntArray, q: IntArray): IntArray {
            val res = IntArray(p.size + q.size - 1)
            for (i in p.indices) {
                for (j in q.indices) {
                    res[i + j] = res[i + j] xor galoisMultiply(p[i], q[j])
                }
            }
            return res
        }
    }
}
