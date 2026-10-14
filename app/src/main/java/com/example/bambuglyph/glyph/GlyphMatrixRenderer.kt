package com.example.bambuglyph.glyph

import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterStatus
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GlyphMatrixRenderer {

    // 3x5 Font definitions (5 rows, 3 bits per row)
    private val FONT_3X5: Map<Char, IntArray> = mapOf(
        '0' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        '3' to intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '6' to intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        '7' to intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010),
        '8' to intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        '9' to intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111),
        '%' to intArrayOf(0b101, 0b001, 0b010, 0b100, 0b101),
        'P' to intArrayOf(0b111, 0b101, 0b111, 0b100, 0b100),
        'H' to intArrayOf(0b101, 0b101, 0b111, 0b101, 0b101),
        'M' to intArrayOf(0b101, 0b111, 0b101, 0b101, 0b101),
        'm' to intArrayOf(0b000, 0b111, 0b101, 0b101, 0b101),
        'h' to intArrayOf(0b100, 0b111, 0b101, 0b101, 0b101),
        ':' to intArrayOf(0b000, 0b010, 0b000, 0b010, 0b000),
        ' ' to intArrayOf(0b000, 0b000, 0b000, 0b000, 0b000),
        '-' to intArrayOf(0b000, 0b000, 0b111, 0b000, 0b000),
        'I' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b111),
        'D' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b110),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'E' to intArrayOf(0b111, 0b100, 0b111, 0b100, 0b111)
    )

    // 5x7 Font definitions for higher resolution 25x25 display
    private val FONT_5X7: Map<Char, IntArray> = mapOf(
        '0' to intArrayOf(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110),
        '1' to intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        '2' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111),
        '3' to intArrayOf(0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110),
        '4' to intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
        '5' to intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110),
        '6' to intArrayOf(0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110),
        '7' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
        '8' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
        '9' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100),
        '%' to intArrayOf(0b11001, 0b11010, 0b00100, 0b01000, 0b01011, 0b10011, 0b00000),
        'P' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000),
        'H' to intArrayOf(0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
        'M' to intArrayOf(0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001),
        'h' to intArrayOf(0b10000, 0b10000, 0b10110, 0b11001, 0b10001, 0b10001, 0b10001),
        'm' to intArrayOf(0b00000, 0b00000, 0b11010, 0b10101, 0b10101, 0b10101, 0b10101)
    )

    fun isInsideCircularMatrix(x: Int, y: Int, size: Int): Boolean {
        val cx = (size - 1) / 2.0
        val cy = (size - 1) / 2.0
        val r = (size - 1) / 2.0
        val dx = x - cx
        val dy = y - cy
        return (dx * dx + dy * dy) <= (r + 0.35) * (r + 0.35)
    }

    private fun getMiddleRadius(size: Int): Double {
        return if (size == 13) 3.6 else 7.5
    }

    fun render(status: PrinterStatus, size: Int, animPhase: Double = 0.0): IntArray {
        val matrix = IntArray(size * size) { 0 }

        when (status.printState) {
            PrintState.PREHEAT -> {
                renderPreheat(matrix, size, animPhase)
            }
            PrintState.PAUSED -> {
                renderPaused(matrix, size, status.percent, animPhase)
            }
            PrintState.COMPLETED -> {
                renderCompletedBreathe(matrix, size, animPhase)
            }
            PrintState.PRINTING -> {
                renderPrinting(matrix, size, status.percent, animPhase)
            }
            PrintState.FAILED -> {
                renderFailed(matrix, size, animPhase)
            }
            PrintState.IDLE -> {
                renderIdle(matrix, size, animPhase)
            }
        }

        // Mask out any pixel physically outside the circular matrix
        maskCircularBoundary(matrix, size)
        return matrix
    }

    /**
     * Printing state:
     * - Fills the outer circular perimeter from 12 o'clock clockwise proportional to percent.
     * - Middle area has black background with centered number percentage (e.g. "45").
     */
    private fun renderPrinting(matrix: IntArray, size: Int, percent: Int, animPhase: Double) {
        val safePercent = percent.coerceIn(0, 100)

        // 1. Fill the outer circular ring based on percentage
        drawCircularProgressRing(matrix, size, safePercent, brightness = 255)

        // 2. Centered number percentage in middle area (with black background around it)
        val percentText = safePercent.toString()
        drawCenteredText(matrix, size, percentText, brightness = 255)
    }

    /**
     * Paused state:
     * - Outer circular ring retains current progress reached.
     * - Middle area has clean solid black background.
     * - Centered pause icon ("||") with gentle breathing pulsation.
     */
    private fun renderPaused(matrix: IntArray, size: Int, percent: Int, animPhase: Double) {
        val safePercent = percent.coerceIn(0, 100)

        // Outer circular ring stays visible with current progress
        drawCircularProgressRing(matrix, size, safePercent, brightness = 210)

        // Middle area: Solid black background with centered pause icon ("||")
        val pulse = (sin(animPhase * 2.0) * 45).toInt()
        val brightness = (210 + pulse).coerceIn(120, 255)

        if (size == 13) {
            // Centered dual vertical bars in middle: x=5, x=7, y=4..8
            for (y in 4..8) {
                setPixel(matrix, size, 5, y, brightness)
                setPixel(matrix, size, 7, y, brightness)
            }
        } else {
            // 25x25: 2-pixel wide bars at x=10..11, 13..14, y=8..16
            for (y in 8..16) {
                setPixel(matrix, size, 10, y, brightness)
                setPixel(matrix, size, 11, y, brightness)
                setPixel(matrix, size, 13, y, brightness)
                setPixel(matrix, size, 14, y, brightness)
            }
        }
    }

    /**
     * Completed state:
     * - Full 100% outer circular ring.
     * - Middle area has black background with centered tick/checkmark breathing smoothly.
     */
    private fun renderCompletedBreathe(matrix: IntArray, size: Int, animPhase: Double) {
        // 100% full circular ring
        drawCircularProgressRing(matrix, size, percent = 100, brightness = 255)

        // Breathing brightness for checkmark (sine wave)
        val breath = (sin(animPhase * 2.5) + 1.0) / 2.0
        val brightness = (35 + (breath * 220)).toInt().coerceIn(35, 255)

        // Centered finished tick with black background around it
        if (size == 13) {
            // Checkmark coordinates in 13x13:
            // Short leg: (3,6) -> (5,8)
            // Long leg:  (5,8) -> (9,4)
            val points = listOf(
                3 to 6, 4 to 7, 5 to 8,
                6 to 7, 7 to 6, 8 to 5, 9 to 4,
                // Extra thickness for clean high-contrast look
                4 to 6, 5 to 7, 6 to 6, 7 to 5, 8 to 4
            )
            for ((x, y) in points) {
                setPixel(matrix, size, x, y, brightness)
            }
        } else {
            // Checkmark in 25x25:
            // Short leg: (6,12) -> (11,17)
            // Long leg:  (11,17) -> (18,7)
            for (i in 0..5) {
                setPixel(matrix, size, 6 + i, 12 + i, brightness)
                setPixel(matrix, size, 6 + i + 1, 12 + i, brightness)
                setPixel(matrix, size, 6 + i, 12 + i + 1, brightness)
            }
            for (i in 0..7) {
                val x = 11 + i
                val y = 17 - (i * 10 / 7)
                setPixel(matrix, size, x, y, brightness)
                setPixel(matrix, size, x + 1, y, brightness)
                setPixel(matrix, size, x, y - 1, brightness)
            }
        }
    }

    /**
     * Preheat state:
     * - Outer circular ring pulses with rotating heat wave.
     * - Middle area has black background with "PH" centered.
     */
    private fun renderPreheat(matrix: IntArray, size: Int, animPhase: Double) {
        val cx = (size - 1) / 2.0
        val cy = (size - 1) / 2.0
        val rMax = (size - 1) / 2.0
        val rMid = getMiddleRadius(size)

        // Circular rotating heat waves on outer ring
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist in rMid..(rMax + 0.35)) {
                    var theta = atan2(dy, dx) + (PI / 2.0)
                    if (theta < 0) theta += 2 * PI
                    val wave = (sin(theta * 3.0 - animPhase * 3.0) + 1.0) / 2.0
                    val waveBrightness = (60 + wave * 180).toInt().coerceIn(0, 255)
                    setPixel(matrix, size, x, y, waveBrightness)
                }
            }
        }

        // Centered "PH" on black background
        val pulse = (sin(animPhase * 3.0) * 35).toInt()
        val textBrightness = (220 + pulse).coerceIn(140, 255)

        if (size == 13) {
            // 'P' at x=3, 'H' at x=7, startY=4 (3x5 font, total width 7, height 5)
            drawGlyph3x5(matrix, size, 'P', startX = 3, startY = 4, textBrightness)
            drawGlyph3x5(matrix, size, 'H', startX = 7, startY = 4, textBrightness)
        } else {
            // 25x25: 5x7 font centered at startY=9
            drawGlyph5x7(matrix, size, 'P', startX = 6, startY = 9, textBrightness)
            drawGlyph5x7(matrix, size, 'H', startX = 14, startY = 9, textBrightness)
        }
    }

    /**
     * Standby/Idle state:
     * - Centered subtle breathing dot, outer ring off.
     */
    private fun renderIdle(matrix: IntArray, size: Int, animPhase: Double) {
        val pulse = (sin(animPhase * 1.5) * 45).toInt()
        val brightness = (100 + pulse).coerceIn(40, 180)

        val cx = (size - 1) / 2
        val cy = (size - 1) / 2

        if (size == 13) {
            // 2x2 or 3x3 subtle center square
            for (y in (cy - 1)..(cy + 1)) {
                for (x in (cx - 1)..(cx + 1)) {
                    setPixel(matrix, size, x, y, brightness)
                }
            }
        } else {
            for (y in (cy - 2)..(cy + 2)) {
                for (x in (cx - 2)..(cx + 2)) {
                    setPixel(matrix, size, x, y, brightness)
                }
            }
        }
    }

    /**
     * Failed state:
     * - Centered 'X' with black background.
     */
    private fun renderFailed(matrix: IntArray, size: Int, animPhase: Double) {
        val brightness = 255
        val cx = (size - 1) / 2
        val cy = (size - 1) / 2
        val span = if (size == 13) 3 else 7

        for (i in -span..span) {
            setPixel(matrix, size, cx + i, cy + i, brightness)
            setPixel(matrix, size, cx - i, cy + i, brightness)
        }
    }

    /**
     * Fills the outer circular perimeter from 12 o'clock clockwise based on percent (0-100%).
     */
    private fun drawCircularProgressRing(matrix: IntArray, size: Int, percent: Int, brightness: Int) {
        if (percent <= 0) return

        val cx = (size - 1) / 2.0
        val cy = (size - 1) / 2.0
        val rMax = (size - 1) / 2.0
        val rMid = getMiddleRadius(size)
        val fillLimit = (percent / 100.0).coerceIn(0.0, 1.0)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)

                if (dist in rMid..(rMax + 0.35)) {
                    // Clockwise angle from 12 o'clock (-PI / 2)
                    var theta = atan2(dy, dx) + (PI / 2.0)
                    if (theta < 0) theta += 2 * PI
                    val fraction = theta / (2 * PI)

                    if (fraction <= fillLimit + 0.005 || percent >= 100) {
                        setPixel(matrix, size, x, y, brightness)
                    }
                }
            }
        }
    }

    /**
     * Draws text centered within the matrix middle area (height 5).
     */
    private fun drawCenteredText(matrix: IntArray, size: Int, text: String, brightness: Int) {
        val totalWidth = text.length * 3 + (text.length - 1) * 1
        val startX = ((size - totalWidth) / 2.0).roundToInt()
        val startY = ((size - 5) / 2.0).roundToInt()

        var currentX = startX
        for (char in text) {
            val rows = FONT_3X5[char] ?: FONT_3X5[' ']!!
            for (rowIdx in 0 until 5) {
                val rowBitmask = rows[rowIdx]
                for (colIdx in 0 until 3) {
                    val isSet = (rowBitmask and (1 shl (2 - colIdx))) != 0
                    if (isSet) {
                        setPixel(matrix, size, currentX + colIdx, startY + rowIdx, brightness)
                    }
                }
            }
            currentX += 4 // 3 width + 1 spacing
        }
    }

    private fun drawGlyph3x5(matrix: IntArray, size: Int, char: Char, startX: Int, startY: Int, brightness: Int) {
        val rows = FONT_3X5[char] ?: return
        for (rowIdx in 0 until 5) {
            val bitmask = rows[rowIdx]
            for (colIdx in 0 until 3) {
                val isSet = (bitmask and (1 shl (2 - colIdx))) != 0
                if (isSet) {
                    setPixel(matrix, size, startX + colIdx, startY + rowIdx, brightness)
                }
            }
        }
    }

    private fun drawGlyph5x7(matrix: IntArray, size: Int, char: Char, startX: Int, startY: Int, brightness: Int) {
        val rows = FONT_5X7[char] ?: return
        for (rowIdx in 0 until 7) {
            val bitmask = rows[rowIdx]
            for (colIdx in 0 until 5) {
                val isSet = (bitmask and (1 shl (4 - colIdx))) != 0
                if (isSet) {
                    setPixel(matrix, size, startX + colIdx, startY + rowIdx, brightness)
                }
            }
        }
    }

    private fun maskCircularBoundary(matrix: IntArray, size: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isInsideCircularMatrix(x, y, size)) {
                    matrix[y * size + x] = 0
                }
            }
        }
    }

    private fun setPixel(matrix: IntArray, size: Int, x: Int, y: Int, brightness: Int) {
        if (x in 0 until size && y in 0 until size) {
            val index = y * size + x
            if (index in matrix.indices) {
                matrix[index] = brightness.coerceIn(0, 255)
            }
        }
    }
}
