package com.example.medtest1.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density

private val LightColorScheme = lightColorScheme(
    primary = MedTeal700,
    onPrimary = White,
    primaryContainer = MedTeal100,
    onPrimaryContainer = MedTeal900,
    secondary = Slate600,
    onSecondary = White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate900,
    tertiary = VioletAccent,
    onTertiary = White,
    tertiaryContainer = VioletContainerLight,
    onTertiaryContainer = VioletOnContainerLight,
    error = Color(0xFFBA1A1A),
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate400,
    outlineVariant = Slate200,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    inversePrimary = MedTeal300,
    surfaceTint = MedTeal700,
    scrim = Black.copy(alpha = 0.32f)
)

private val DarkColorScheme = darkColorScheme(
    primary = MedTeal300,
    onPrimary = Color(0xFF003734),
    primaryContainer = MedTeal800,
    onPrimaryContainer = MedTeal100,
    secondary = Slate300,
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate500,
    outlineVariant = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    inversePrimary = MedTeal800,
    surfaceTint = MedTeal300,
    scrim = Black.copy(alpha = 0.5f)
)

@Composable
fun Medtest1Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    forceStableFontScale: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val density = LocalDensity.current
    val fixedDensity = if (forceStableFontScale) {
        Density(density = density.density, fontScale = 1f)
    } else {
        density
    }
    val uiMetrics = rememberUiMetrics()
    val medAppColors = rememberMedAppColors(darkTheme, colorScheme)

    CompositionLocalProvider(LocalDensity provides fixedDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (uiMetrics.isCompact) CompactTypography else Typography
        ) {
            CompositionLocalProvider(LocalMedAppColors provides medAppColors) {
                content()
            }
        }
    }
}
