package com.example.bambuglyph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bambuglyph.theme.BorderDark
import com.example.bambuglyph.theme.CardDark
import com.example.bambuglyph.theme.LedOff
import com.example.bambuglyph.theme.NothingBlack
import com.example.bambuglyph.theme.NothingRed
import com.example.bambuglyph.theme.TextMuted
import com.example.bambuglyph.theme.TextSecondary

@Composable
fun GlyphMatrixView(
    matrixData: IntArray,
    matrixSize: Int,
    modifier: Modifier = Modifier,
    label: String = if (matrixSize == 25) "NOTHING PHONE (3) • 25x25" else "PHONE (4a) PRO • 13x13"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                )
                Text(
                    text = " GLYPH MATRIX SIMULATOR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            )
        }

        // The Circular LED Matrix Container (Nothing Phone rear glass motif)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(NothingBlack)
                .border(2.dp, BorderDark, CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.aspectRatio(1f)) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val cellWidth = canvasWidth / matrixSize
                val cellHeight = canvasHeight / matrixSize
                val dotRadius = (cellWidth.coerceAtMost(cellHeight) * 0.38f)

                for (y in 0 until matrixSize) {
                    for (x in 0 until matrixSize) {
                        if (!com.example.bambuglyph.glyph.GlyphMatrixRenderer.isInsideCircularMatrix(x, y, matrixSize)) {
                            continue
                        }
                        val index = y * matrixSize + x
                        val brightness = if (index in matrixData.indices) matrixData[index].coerceIn(0, 255) else 0

                        val centerX = x * cellWidth + cellWidth / 2f
                        val centerY = y * cellHeight + cellHeight / 2f

                        if (brightness > 0) {
                            val alpha = brightness / 255f
                            // Outer LED Glow
                            if (brightness > 60) {
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha * 0.28f),
                                    radius = dotRadius * 1.6f,
                                    center = Offset(centerX, centerY)
                                )
                            }
                            // Inner Bright Core
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = dotRadius,
                                center = Offset(centerX, centerY)
                            )
                        } else {
                            // Unlit LED
                            drawCircle(
                                color = LedOff,
                                radius = dotRadius * 0.85f,
                                center = Offset(centerX, centerY)
                            )
                        }
                    }
                }
            }
        }
    }
}
