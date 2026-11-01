package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = GoldContainer,
    onPrimaryContainer = OnGoldContainer,
    secondary = GoldDark,
    onSecondary = Color.White,
    tertiary = GoldLight,
    onTertiary = Color(0xFF0F172A),
    background = VaultBackground,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = VaultBorder,
    error = VaultError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = Typography,
        content = content
    )
}
