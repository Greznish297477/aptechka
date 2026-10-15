package com.medexpiry.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.medexpiry.app.data.ColorStyle
import com.medexpiry.app.data.TextSizeOption
import com.medexpiry.app.data.ThemeMode

private data class Palette(
    val primary: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color
)

private fun palette(style: ColorStyle): Palette = when (style) {
    ColorStyle.INDIGO -> Palette(
        Color(0xFF3F51B5), Color(0xFF7986CB), Color(0xFFE8EAF6), Color(0xFF1A237E),
        Color(0xFF00897B), Color(0xFFB2DFDB), Color(0xFF004D40)
    )
    ColorStyle.GREEN -> Palette(
        Color(0xFF2E7D32), Color(0xFF66BB6A), Color(0xFFE8F5E9), Color(0xFF1B5E20),
        Color(0xFF43A047), Color(0xFFC8E6C9), Color(0xFF1B5E20)
    )
    ColorStyle.TEAL -> Palette(
        Color(0xFF00897B), Color(0xFF4DB6AC), Color(0xFFE0F2F1), Color(0xFF004D40),
        Color(0xFF00838F), Color(0xFFB2EBF2), Color(0xFF006064)
    )
    ColorStyle.PURPLE -> Palette(
        Color(0xFF7B1FA2), Color(0xFFBA68C8), Color(0xFFF3E5F5), Color(0xFF4A148C),
        Color(0xFF8E24AA), Color(0xFFE1BEE7), Color(0xFF4A148C)
    )
    ColorStyle.ORANGE -> Palette(
        Color(0xFFEF6C00), Color(0xFFFFB74D), Color(0xFFFFF3E0), Color(0xFFE65100),
        Color(0xFFF57C00), Color(0xFFFFE0B2), Color(0xFFE65100)
    )
    ColorStyle.BLUE -> Palette(
        Color(0xFF1565C0), Color(0xFF42A5F5), Color(0xFFE3F2FD), Color(0xFF0D47A1),
        Color(0xFF0277BD), Color(0xFFB3E5FC), Color(0xFF01579B)
    )
    ColorStyle.RED -> Palette(
        Color(0xFFC62828), Color(0xFFEF5350), Color(0xFFFFEBEE), Color(0xFFB71C1C),
        Color(0xFFD32F2F), Color(0xFFFFCDD2), Color(0xFFB71C1C)
    )
    ColorStyle.PINK -> Palette(
        Color(0xFFC2185B), Color(0xFFF06292), Color(0xFFFCE4EC), Color(0xFF880E4F),
        Color(0xFFE91E63), Color(0xFFF8BBD0), Color(0xFF880E4F)
    )
    ColorStyle.CYAN -> Palette(
        Color(0xFF00838F), Color(0xFF26C6DA), Color(0xFFE0F7FA), Color(0xFF006064),
        Color(0xFF00ACC1), Color(0xFFB2EBF2), Color(0xFF006064)
    )
    ColorStyle.AMBER -> Palette(
        Color(0xFFFF8F00), Color(0xFFFFD54F), Color(0xFFFFF8E1), Color(0xFFFF6F00),
        Color(0xFFFFA000), Color(0xFFFFECB3), Color(0xFFFF6F00)
    )
    ColorStyle.MINT -> Palette(
        Color(0xFF00897B), Color(0xFF80CBC4), Color(0xFFE0F2F1), Color(0xFF004D40),
        Color(0xFF26A69A), Color(0xFFB2DFDB), Color(0xFF004D40)
    )
    ColorStyle.CORAL -> Palette(
        Color(0xFFE64A19), Color(0xFFFF8A65), Color(0xFFFBE9E7), Color(0xFFBF360C),
        Color(0xFFFF7043), Color(0xFFFFCCBC), Color(0xFFBF360C)
    )
    ColorStyle.BROWN -> Palette(
        Color(0xFF6D4C41), Color(0xFFA1887F), Color(0xFFEFEBE9), Color(0xFF3E2723),
        Color(0xFF795548), Color(0xFFD7CCC8), Color(0xFF3E2723)
    )
    ColorStyle.SLATE -> Palette(
        Color(0xFF455A64), Color(0xFF90A4AE), Color(0xFFECEFF1), Color(0xFF263238),
        Color(0xFF607D8B), Color(0xFFCFD8DC), Color(0xFF263238)
    )
}

@Composable
fun MedExpiryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorStyle: ColorStyle = ColorStyle.INDIGO,
    textSize: TextSizeOption = TextSizeOption.NORMAL,
    customPrimaryColor: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val p = if (customPrimaryColor != 0) {
        paletteFromColor(Color(customPrimaryColor))
    } else {
        palette(colorStyle)
    }

    val colors = if (darkTheme) {
        darkColorScheme(
            primary = p.primaryLight,
            onPrimary = p.onPrimaryContainer,
            primaryContainer = p.primary,
            onPrimaryContainer = Color(0xFFE8EAF6),
            secondary = p.secondary,
            onSecondary = Color.Black,
            secondaryContainer = p.onSecondaryContainer,
            onSecondaryContainer = p.secondaryContainer,
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
    } else {
        lightColorScheme(
            primary = p.primary,
            onPrimary = Color.White,
            primaryContainer = p.primaryContainer,
            onPrimaryContainer = p.onPrimaryContainer,
            secondary = p.secondary,
            onSecondary = Color.White,
            secondaryContainer = p.secondaryContainer,
            onSecondaryContainer = p.onSecondaryContainer,
            tertiary = p.primaryLight,
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
    }

    val scale = when (textSize) {
        TextSizeOption.SMALL -> 0.90f
        TextSizeOption.NORMAL -> 1.0f
        TextSizeOption.LARGE -> 1.15f
    }
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontSize = base.displayLarge.fontSize * scale),
        displayMedium = base.displayMedium.copy(fontSize = base.displayMedium.fontSize * scale),
        displaySmall = base.displaySmall.copy(fontSize = base.displaySmall.fontSize * scale),
        headlineLarge = base.headlineLarge.copy(fontSize = base.headlineLarge.fontSize * scale),
        headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize * scale),
        headlineSmall = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize * scale),
        titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * scale),
        titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * scale),
        titleSmall = base.titleSmall.copy(fontSize = base.titleSmall.fontSize * scale),
        bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * scale),
        bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * scale),
        bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize * scale),
        labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * scale),
        labelMedium = base.labelMedium.copy(fontSize = base.labelMedium.fontSize * scale),
        labelSmall = base.labelSmall.copy(fontSize = base.labelSmall.fontSize * scale)
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

object StatusColors {
    val Safe = Color(0xFF00897B)
    val Warning = Color(0xFFFB8C00)
    val Critical = Color(0xFFE64A19)
    val Expired = Color(0xFFD32F2F)
}
