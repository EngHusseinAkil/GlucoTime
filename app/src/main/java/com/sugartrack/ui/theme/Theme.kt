package com.sugartrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlue.copy(alpha = 0.15f),
    onPrimaryContainer = RoyalBlue,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8FAFF),
    onSecondaryContainer = Navy,
    tertiary = Coral,
    onTertiary = Color.White,
    background = OffWhite,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = CoolGray,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = LightBlue,
    onPrimary = Navy,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = LightBlue,
    secondary = ElectricBlue,
    onSecondary = Navy,
    secondaryContainer = NavyVariant,
    onSecondaryContainer = ElectricBlue,
    tertiary = Coral,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = OffWhite,
    surface = DarkSurface,
    onSurface = OffWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = NavyVariant,
    outlineVariant = DarkSurfaceVariant
)

@Composable
fun GlucoTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}
