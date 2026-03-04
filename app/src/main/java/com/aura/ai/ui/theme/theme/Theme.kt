package com.aura.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PremiumPrimary,
    secondary = PremiumSecondary,
    tertiary = PremiumGradient2,
    background = PremiumBackground,
    surface = PremiumSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = PremiumOnBackground,
    onSurface = PremiumOnSurface,
    error = PremiumError,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumPrimary,
    secondary = PremiumSecondary,
    tertiary = PremiumGradient2,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = PremiumError,
    onError = Color.White
)

@Composable
fun AuraAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PremiumTypography,
        shapes = PremiumShapes,
        content = content
    )
}

val PremiumShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
