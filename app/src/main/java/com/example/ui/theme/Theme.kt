package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndiaSaffron,
    onPrimary = Color(0xFF2B1000),
    primaryContainer = Color(0xFF5A2600),
    onPrimaryContainer = IndiaSaffronLight,
    secondary = IndiaGreen,
    onSecondary = Color(0xFF002202),
    secondaryContainer = Color(0xFF083D04),
    onSecondaryContainer = IndiaGreenLight,
    tertiary = IndiaMarigold,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF6B3600),
    onTertiaryContainer = ProGoldLight,
    background = WhatsAppDarkBackground,
    onBackground = Color(0xFFE6EDF5),
    surface = WhatsAppDarkSurface,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF9CB1C9),
    outline = WhatsAppDarkOutline,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IndiaSaffronDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF461500),
    secondary = IndiaGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFF6BF),
    onSecondaryContainer = Color(0xFF023602),
    tertiary = IndiaChakraBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E4FF),
    onTertiaryContainer = Color(0xFF001A42),
    background = WhatsAppLightBackground,
    onBackground = Color(0xFF1B1611),
    surface = WhatsAppLightSurface,
    onSurface = Color(0xFF1B1611),
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5D5144),
    outline = WhatsAppLightOutline,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinct Indian Theme branding consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
