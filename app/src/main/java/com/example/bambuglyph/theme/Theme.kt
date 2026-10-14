package com.example.bambuglyph.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NothingColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    primaryContainer = NothingRedSubtle,
    onPrimaryContainer = NothingWhite,
    secondary = TextSecondary,
    onSecondary = NothingWhite,
    background = NothingBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark
)

@Composable
fun BambuGlyphTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NothingColorScheme,
        typography = Typography,
        content = content
    )
}
