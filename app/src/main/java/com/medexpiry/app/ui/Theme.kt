package com.medexpiry.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Новая палитра — мягкий индиго + бирюза (современный медицинский стиль)
private val IndigoPrimary = Color(0xFF3F51B5)
private val IndigoLight = Color(0xFF7986CB)
private val TealAccent = Color(0xFF00897B)
private val IndigoContainer = Color(0xFFE8EAF6)

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = Color(0xFF5C6BC0),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFEBEE),
    background = Color(0xFFF5F6FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF0F7),
    onBackground = Color(0xFF1A1B23),
    onSurface = Color(0xFF1A1B23),
    onSurfaceVariant = Color(0xFF4A4C5A),
    outline = Color(0xFFB0B3C0)
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF303F9F),
    onPrimaryContainer = Color(0xFFC5CAE9),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003D36),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFB2DFDB),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFF5C1010),
    background = Color(0xFF12131A),
    surface = Color(0xFF1C1D27),
    surfaceVariant = Color(0xFF2A2C3A),
    onBackground = Color(0xFFE4E5ED),
    onSurface = Color(0xFFE4E5ED),
    onSurfaceVariant = Color(0xFFB0B3C0),
    outline = Color(0xFF5A5D6C)
)

@Composable
fun MedExpiryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}

object StatusColors {
    val Safe = Color(0xFF00897B)      // бирюза
    val Warning = Color(0xFFFB8C00)   // янтарный
    val Critical = Color(0xFFE64A19)  // глубокий оранжевый
    val Expired = Color(0xFFD32F2F)   // красный
}
