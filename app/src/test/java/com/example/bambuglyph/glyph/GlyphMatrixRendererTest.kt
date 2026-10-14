package com.example.bambuglyph.glyph

import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphMatrixRendererTest {

    @Test
    fun testCircularMasking() {
        val status = PrinterStatus(
            printState = PrintState.COMPLETED,
            remainingMinutes = 0,
            percent = 100,
            gcodeState = "FINISH"
        )
        val matrix25 = GlyphMatrixRenderer.render(status, size = 25, animPhase = 0.0)
        // Corners must be strictly 0 because matrix is circular
        assertEquals(0, matrix25[0]) // (0, 0)
        assertEquals(0, matrix25[24]) // (24, 0)
        assertEquals(0, matrix25[24 * 25]) // (0, 24)
        assertEquals(0, matrix25[24 * 25 + 24]) // (24, 24)

        // Center must be inside circle
        assertTrue(GlyphMatrixRenderer.isInsideCircularMatrix(12, 12, 25))
        assertFalse(GlyphMatrixRenderer.isInsideCircularMatrix(0, 0, 25))
    }

    @Test
    fun testMatrixDimensionsPhone4aPro() {
        val status = PrinterStatus(
            printState = PrintState.PRINTING,
            remainingMinutes = 20,
            percent = 50,
            gcodeState = "RUNNING"
        )
        val matrix13 = GlyphMatrixRenderer.render(status, size = 13, animPhase = 0.0)
        assertEquals(13 * 13, matrix13.size)
        // Corners outside circle are 0
        assertEquals(0, matrix13[0])
        assertEquals(0, matrix13[12])
        // Centered percentage or circular ring pixels are illuminated
        assertTrue(matrix13.any { it > 0 })
    }

    @Test
    fun testPausedIconAndBlackBackground() {
        val status = PrinterStatus(
            printState = PrintState.PAUSED,
            remainingMinutes = 30,
            percent = 60,
            gcodeState = "PAUSE"
        )
        val matrix = GlyphMatrixRenderer.render(status, size = 13, animPhase = 0.0)
        // Center bars at x=5, y=6 and x=7, y=6 are illuminated
        assertTrue(matrix[6 * 13 + 5] > 0)
        assertTrue(matrix[6 * 13 + 7] > 0)
        // Middle gap at x=6, y=6 is black (0)
        assertEquals(0, matrix[6 * 13 + 6])
    }

    @Test
    fun testCompletedFinishedTick() {
        val status = PrinterStatus(
            printState = PrintState.COMPLETED,
            remainingMinutes = 0,
            percent = 100,
            gcodeState = "FINISH"
        )
        val matrix = GlyphMatrixRenderer.render(status, size = 13, animPhase = 1.0)
        // Checkmark tip at (5,8) should be lit
        assertTrue(matrix[8 * 13 + 5] > 0)
    }

    @Test
    fun testPrintingCenteredPercent() {
        val status = PrinterStatus(
            printState = PrintState.PRINTING,
            remainingMinutes = 15,
            percent = 85,
            gcodeState = "RUNNING"
        )
        val matrix = GlyphMatrixRenderer.render(status, size = 13, animPhase = 0.0)
        // Text is rendered in center rows
        var centerLitCount = 0
        for (y in 4..8) {
            for (x in 3..9) {
                if (matrix[y * 13 + x] > 0) centerLitCount++
            }
        }
        assertTrue("Expected centered percentage digits to be illuminated", centerLitCount > 0)
    }
}
