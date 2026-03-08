package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryGreen = Color(0xFF0F8B7A)

private val BrandBlue = Color(0xFF1E3A8A)

private val PageBg = Color.White
//private val PageBg = Color(0xFF4E5259)
private val SurfaceWhite = Color(0xFFFFFFFF)

private val TextMain = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)

//Color(0xFFE3E8F0)
private val Outline = Color(0xFFCFD4DE)

private val SoftButton = Color(0xFFE9EEF6)

private val ErrorRed = Color(0xFFEF4444)
private val ErrorBg = Color(0xFFFFEBEE)
private val ErrorText = Color(0xFFB71C1C)

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,

    secondary = BrandBlue,
    onSecondary = Color.White,

    background = PageBg,
    onBackground = TextMain,

    surface = SurfaceWhite,
    onSurface = TextMain,

    surfaceVariant = SoftButton,
    onSurfaceVariant = TextMuted,

    outline = Outline,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorBg,
    onErrorContainer = ErrorText
)

// Dark
private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF062B27),

    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0A1B3D),

    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE5E7EB),

    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE5E7EB),

    surfaceVariant = Color(0xFF111C33),
    onSurfaceVariant = Color(0xFF94A3B8),

    outline = Color(0xFF23304A),

    error = Color(0xFFF87171),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF3B0A0A),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun Shre2FixTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
