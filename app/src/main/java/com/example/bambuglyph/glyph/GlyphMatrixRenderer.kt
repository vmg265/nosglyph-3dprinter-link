package com.example.bambuglyph.glyph

import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterStatus
import kotlin.math.PI
import kotlin.math.sin

object GlyphMatrixRenderer {

    // 3x5 Font definitions (binary encoding of 5 rows, 3 bits per row)
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
        'P' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000),
        'H' to intArrayOf(0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
        'M' to intArrayOf(0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001),
        'h' to intArrayOf(0b10000, 0b10000, 0b10110, 0b11001, 0b10001, 0b10001, 0b10001),
        'm' to intArrayOf(0b00000, 0b00000, 0b11010, 0b10101, 0b10101, 0b10101, 0b10101)
    )

    fun render(status: PrinterStatus, size: Int, animPhase: Double = 0.0): IntArray {
        val matrix = IntArray(size * size) { 0 }

        when (status.printState) {
            PrintState.PREHEAT -> {
                renderPreheat(matrix, size, animPhase)
            }
            PrintState.PAUSED -> {
                renderPaused(matrix, size, animPhase)
            }
            PrintState.COMPLETED -> {
                renderCompletedBreathe(matrix, size, animPhase)
            }
            PrintState.PRINTING -> {
                renderPrinting(matrix, size, status.remainingMinutes, status.percent, animPhase)
            }
            PrintState.FAILED -> {
                renderFailed(matrix, size, animPhase)
            }
            PrintState.IDLE -> {
                renderIdle(matrix, size, animPhase)
            }
        }

        return matrix
    }

    /**
     * Renders "PH" for preheating state with subtle heating pulse.
     */
    private fun renderPreheat(matrix: IntArray, size: Int, animPhase: Double) {
        val pulse = (sin(animPhase * 3.0) * 40).toInt()
        val brightness = (215 + pulse).coerceIn(120, 255)

        if (size == 13) {
            // Draw 'P' at x=1, y=3; 'H' at x=7, y=3 (5x7 font)
            drawGlyph5x7(matrix, size, 'P', startX = 1, startY = 3, brightness)
            drawGlyph5x7(matrix, size, 'H', startX = 7, startY = 3, brightness)

            // Heat waves along bottom (rows 11-12)
            val waveOffset = ((animPhase * 2.0).toInt()) % 3
            for (x in 2..10 step 2) {
                val waveX = (x + waveOffset) % 11 + 1
                setPixel(matrix, size, waveX, 11, 140)
            }
        } else {
            // 25x25 matrix: larger spacing
            drawGlyph5x7(matrix, size, 'P', startX = 5, startY = 8, brightness)
            drawGlyph5x7(matrix, size, 'H', startX = 14, startY = 8, brightness)

            // Double line heat wave at bottom
            val waveOffset = ((animPhase * 2.5).toInt()) % 4
            for (x in 3..21) {
                val yOffset = if ((x + waveOffset) % 4 < 2) 20 else 21
                setPixel(matrix, size, x, yOffset, 160)
            }
        }
    }

    /**
     * Renders standard Pause icon (two vertical bars "||").
     */
    private fun renderPaused(matrix: IntArray, size: Int, animPhase: Double) {
        // Slow gentle pulse
        val pulse = (sin(animPhase * 1.5) * 35).toInt()
        val brightness = (220 + pulse).coerceIn(140, 255)

        if (size == 13) {
            // Bar 1: x=4..5, y=3..9
            // Bar 2: x=7..8, y=3..9
            for (y in 3..9) {
                setPixel(matrix, size, 4, y, brightness)
                setPixel(matrix, size, 5, y, brightness)
                setPixel(matrix, size, 7, y, brightness)
                setPixel(matrix, size, 8, y, brightness)
            }
        } else {
            // 25x25:
            // Bar 1: x=8..10, y=6..18
            // Bar 2: x=14..16, y=6..18
            for (y in 6..18) {
                for (x in 8..10) setPixel(matrix, size, x, y, brightness)
                for (x in 14..16) setPixel(matrix, size, x, y, brightness)
            }
        }
    }

    /**
     * Renders 100% completion tick with breathing effect (smooth brightness pulsation).
     */
    private fun renderCompletedBreathe(matrix: IntArray, size: Int, animPhase: Double) {
        // Breath factor between 0.0 and 1.0 using sine wave
        val breath = (sin(animPhase * 2.2) + 1.0) / 2.0
        // Pulsate brightness between 25 and 255
        val brightness = (25 + (breath * 230)).toInt().coerceIn(25, 255)

        if (size == 13) {
            // Checkmark coordinates for 13x13:
            // Short leg: (2,6) -> (3,7) -> (4,8) -> (5,9)
            // Long leg:  (5,9) -> (6,8) -> (7,7) -> (8,6) -> (9,5) -> (10,4)
            val points = listOf(
                2 to 6, 3 to 7, 4 to 8, 5 to 9,
                6 to 8, 7 to 7, 8 to 6, 9 to 5, 10 to 4,
                // Make 2 pixels thick for prominent look
                3 to 6, 4 to 7, 5 to 8, 6 to 7, 7 to 6, 8 to 5, 9 to 4
            )
            for ((x, y) in points) {
                setPixel(matrix, size, x, y, brightness)
            }
        } else {
            // Checkmark coordinates for 25x25:
            // Short leg: (4,12) -> (10,18)
            // Long leg:  (10,18) -> (20,6)
            for (i in 0..6) {
                setPixel(matrix, size, 4 + i, 12 + i, brightness)
                setPixel(matrix, size, 4 + i + 1, 12 + i, brightness)
                setPixel(matrix, size, 4 + i, 12 + i + 1, brightness)
            }
            for (i in 0..10) {
                setPixel(matrix, size, 10 + i, 18 - (i * 12 / 10), brightness)
                setPixel(matrix, size, 10 + i + 1, 18 - (i * 12 / 10), brightness)
                setPixel(matrix, size, 10 + i, 17 - (i * 12 / 10), brightness)
            }

            // Outer border subtle sparkle/glow when completed
            val borderBrightness = (brightness * 0.35).toInt()
            drawBorderProgress(matrix, size, percent = 100, borderBrightness)
        }
    }

    /**
     * Renders remaining print time (e.g. "45m", "1h20") and progress border ring.
     */
    private fun renderPrinting(matrix: IntArray, size: Int, remainingMinutes: Int, percent: Int, animPhase: Double) {
        val brightness = 255

        // Draw progress perimeter ring
        val borderBrightness = 140
        drawBorderProgress(matrix, size, percent, borderBrightness)

        // Text representation
        val text = formatRemainingTime(remainingMinutes)

        if (size == 13) {
            // In 13x13: 3x5 font fits up to 3 chars cleanly
            val compactText = if (remainingMinutes >= 60) {
                val h = remainingMinutes / 60
                val m = remainingMinutes % 60
                if (h >= 10) "${h}h" else "${h}h"
            } else {
                if (remainingMinutes < 10) " ${remainingMinutes}m" else "${remainingMinutes}m"
            }
            drawText3x5(matrix, size, compactText.trim(), startY = 4, brightness)
        } else {
            // In 25x25: spacious! We can draw formatted remaining time
            if (remainingMinutes >= 60) {
                val h = remainingMinutes / 60
                val m = remainingMinutes % 60
                val line = if (m > 0) "${h}h${m.toString().padStart(2, '0')}" else "${h}h"
                drawText3x5(matrix, size, line, startY = 10, brightness)
            } else {
                val minStr = "${remainingMinutes}m"
                drawText3x5(matrix, size, minStr, startY = 10, brightness)
            }
        }
    }

    /**
     * Renders standby/idle icon.
     */
    private fun renderIdle(matrix: IntArray, size: Int, animPhase: Double) {
        val pulse = (sin(animPhase * 1.5) * 50).toInt()
        val brightness = (100 + pulse).coerceIn(40, 180)

        if (size == 13) {
            // Centered minimalist 3x3 square
            for (y in 5..7) {
                for (x in 5..7) {
                    setPixel(matrix, size, x, y, brightness)
                }
            }
        } else {
            // 25x25: subtle printer silhouette or centered 5x5 square
            for (y in 10..14) {
                for (x in 10..14) {
                    setPixel(matrix, size, x, y, brightness)
                }
            }
            // 4 corner marker dots
            setPixel(matrix, size, 4, 4, 80)
            setPixel(matrix, size, 20, 4, 80)
            setPixel(matrix, size, 4, 20, 80)
            setPixel(matrix, size, 20, 20, 80)
        }
    }

    /**
     * Renders Failed/Error icon (an 'X').
     */
    private fun renderFailed(matrix: IntArray, size: Int, animPhase: Double) {
        val brightness = 255
        if (size == 13) {
            for (i in 3..9) {
                setPixel(matrix, size, i, i, brightness)
                setPixel(matrix, size, 12 - i, i, brightness)
            }
        } else {
            for (i in 6..18) {
                setPixel(matrix, size, i, i, brightness)
                setPixel(matrix, size, i + 1, i, brightness)
                setPixel(matrix, size, 24 - i, i, brightness)
                setPixel(matrix, size, 25 - i, i, brightness)
            }
        }
    }

    /**
     * Draws an outer border progress ring around the matrix edge corresponding to percent (0-100%).
     */
    private fun drawBorderProgress(matrix: IntArray, size: Int, percent: Int, brightness: Int) {
        if (percent <= 0) return

        // Total perimeter LEDs: (size - 1) * 4
        val totalPerimeter = (size - 1) * 4
        val activeCount = ((percent / 100.0) * totalPerimeter).toInt().coerceIn(0, totalPerimeter)

        // Traversal order: Top (L to R) -> Right (T to B) -> Bottom (R to L) -> Left (B to T)
        var count = 0

        // Top edge: (0,0) to (size-1, 0)
        for (x in 0 until size - 1) {
            if (count >= activeCount) return
            setPixel(matrix, size, x, 0, brightness)
            count++
        }

        // Right edge: (size-1, 0) to (size-1, size-1)
        for (y in 0 until size - 1) {
            if (count >= activeCount) return
            setPixel(matrix, size, size - 1, y, brightness)
            count++
        }

        // Bottom edge: (size-1, size-1) to (0, size-1)
        for (x in size - 1 downTo 1) {
            if (count >= activeCount) return
            setPixel(matrix, size, x, size - 1, brightness)
            count++
        }

        // Left edge: (0, size-1) to (0, 0)
        for (y in size - 1 downTo 1) {
            if (count >= activeCount) return
            setPixel(matrix, size, 0, y, brightness)
            count++
        }
    }

    private fun drawText3x5(matrix: IntArray, size: Int, text: String, startY: Int, brightness: Int) {
        val totalWidth = text.length * 3 + (text.length - 1) * 1
        var currentX = (size - totalWidth) / 2

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

    private fun setPixel(matrix: IntArray, size: Int, x: Int, y: Int, brightness: Int) {
        if (x in 0 until size && y in 0 until size) {
            val index = y * size + x
            if (index in matrix.indices) {
                matrix[index] = brightness.coerceIn(0, 255)
            }
        }
    }

    private fun formatRemainingTime(minutes: Int): String {
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            "${h}h${m}m"
        } else {
            "${minutes}m"
        }
    }
}
