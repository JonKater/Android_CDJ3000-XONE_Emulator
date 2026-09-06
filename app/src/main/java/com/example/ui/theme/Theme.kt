package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DjDarkColorScheme = darkColorScheme(
    primary = DjDeckACyan,
    onPrimary = Color.Black,
    secondary = DjDeckBOrange,
    onSecondary = Color.Black,
    tertiary = DjPlayGreen,
    background = DjChassisDark,
    onBackground = DjTextPrimary,
    surface = DjPanelDark,
    onSurface = DjTextPrimary,
    surfaceVariant = DjPanelElevated,
    onSurfaceVariant = DjTextSecondary,
    outline = DjPanelBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DjDarkColorScheme,
        typography = Typography,
        content = content
    )
}

