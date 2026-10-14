package com.example.bambuglyph.glyph

import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphMatrixRendererTest {

    @Test
    fun testMatrixDimensionsPhone3() {
        val status = PrinterStatus(
            printState = PrintState.PREHEAT,
            remainingMinutes = 45,
            percent = 10,
            gcodeState = "PREHEAT"
        )
        val matrix25 = GlyphMatrixRenderer.render(status, size = 25, animPhase = 0.0)
        assertEquals(25 * 25, matrix25.size)
        // Ensure at least some pixels are illuminated
        assertTrue(matrix25.any { it > 0 })
    }

    @Test
    fun testMatrixDimensionsPhone4aPro() {
        val status = PrinterStatus(
            printState = PrintState.PRINTING,
            remainingMinutes = 20,
            percent = 40,
            gcodeState = "RUNNING"
        )
        val matrix13 = GlyphMatrixRenderer.render(status, size = 13, animPhase = 0.0)
        assertEquals(13 * 13, matrix13.size)
        assertTrue(matrix13.any { it > 0 })
    }

    @Test
    fun testPausedIconRendered() {
        val status = PrinterStatus(
            printState = PrintState.PAUSED,
            remainingMinutes = 30,
            percent = 50,
            gcodeState = "PAUSE"
        )
        val matrix = GlyphMatrixRenderer.render(status, size = 13, animPhase = 0.0)
        // Two vertical bars in middle
        assertTrue(matrix.any { it > 100 })
    }

    @Test
    fun testCompletedBreatheRendered() {
        val status = PrinterStatus(
            printState = PrintState.COMPLETED,
            remainingMinutes = 0,
            percent = 100,
            gcodeState = "FINISH"
        )
        val matrix = GlyphMatrixRenderer.render(status, size = 25, animPhase = 1.0)
        assertTrue(matrix.any { it > 0 })
    }
}
