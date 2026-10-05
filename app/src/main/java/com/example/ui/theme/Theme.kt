package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 'Royal Heritage' Material 3 Light Theme
 * Luminous, sophisticated literary atmosphere with warm cream, pristine ivory, ruby maroon, and refined gold.
 */
private val DarkRoyalHeritageColorScheme = darkColorScheme(
    primary = DarkRoseGoldPrimary,
    onPrimary = Color(0xFF42000A),
    primaryContainer = DarkMaroonPrimaryContainer,
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = DarkLuminousGoldSecondary,
    onSecondary = Color(0xFF382700),
    secondaryContainer = DarkGoldSecondaryContainer,
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = DarkSandalwoodTertiary,
    onTertiary = Color(0xFF2E1500),
    background = VelvetMaroonBgDark,
    onBackground = VelvetMaroonOnSurfaceDark,
    surface = VelvetMaroonSurfaceDark,
    onSurface = VelvetMaroonOnSurfaceDark,
    surfaceVariant = VelvetMaroonSurfaceVariantDark,
    onSurfaceVariant = VelvetMaroonOnSurfaceVariantDark,
    outline = VelvetMaroonOutlineDark
)

private val LightRoyalHeritageColorScheme = lightColorScheme(
    primary = RoyalMaroonPrimary,
    onPrimary = Color.White,
    primaryContainer = RoyalMaroonPrimaryContainer,
    onPrimaryContainer = RoyalMaroonOnPrimaryContainer,
    secondary = HeritageGoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = HeritageGoldSecondaryContainer,
    onSecondaryContainer = HeritageGoldOnSecondaryContainer,
    tertiary = RoyalSandalwoodTertiary,
    onTertiary = Color.White,
    tertiaryContainer = RoyalSandalwoodTertiaryContainer,
    onTertiaryContainer = RoyalSandalwoodOnTertiaryContainer,
    background = HeritageCreamBgLight,
    onBackground = HeritageOnSurfaceLight,
    surface = HeritageIvorySurfaceLight,
    onSurface = HeritageOnSurfaceLight,
    surfaceVariant = HeritageCreamSurfaceVariantLight,
    onSurfaceVariant = HeritageOnSurfaceVariantLight,
    outline = HeritageOutlineLight,
    outlineVariant = HeritageOutlineVariantLight
)

@Composable
fun PremchandTheme(
    darkTheme: Boolean = false, // Defaults strictly to Light Mode for a bright, readable aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkRoyalHeritageColorScheme else LightRoyalHeritageColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
