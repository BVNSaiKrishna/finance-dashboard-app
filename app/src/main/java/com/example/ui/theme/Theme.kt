package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B0066),
    onSecondaryContainer = Color(0xFFF1DBFF),
    tertiary = ElectricYellow,
    onTertiary = Color.Black,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    error = FinanceRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF742ECB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1DBFF),
    onSecondaryContainer = Color(0xFF24005A),
    tertiary = Color(0xFF6E5E00),
    background = Color(0xFFF6F9FF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE0E3E8),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun PulseFinanceTheme(
    darkTheme: Boolean = true, // Default to premium dark theme!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
