package com.example.medtest1.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

@Immutable
data class MedAppColors(
    val heroGradient: Brush,
    val onHero: Color,
    val onHeroMuted: Color,
    val cardOnHero: Color,
    val cardOnHeroSecondary: Color,
    val accentRing: Color,
    val accentRingSoft: Color,
    val scannerIdle: Color,
    val scannerBusy: Color,
    val tabPlanSelected: Color,
    val tabDiarySelected: Color,
    val tabNextSelected: Color,
    val tabUnselected: Color,
    val snackbarSuccess: Color,
    val snackbarInfo: Color,
    val onSnackbar: Color,
    val illustrationPackDeep: Color,
    val illustrationPackMid: Color,
    val illustrationPackAccent: Color,
    val illustrationPillGreenStart: Color,
    val illustrationPillGreenEnd: Color
)

val LocalMedAppColors = staticCompositionLocalOf<MedAppColors> {
    error("MedAppColors not provided — wrap UI in Medtest1Theme")
}

@Composable
fun rememberMedAppColors(darkTheme: Boolean, scheme: ColorScheme): MedAppColors {
    return remember(darkTheme, scheme) {
        if (darkTheme) medAppColorsDark(scheme) else medAppColorsLight(scheme)
    }
}

private fun medAppColorsLight(scheme: ColorScheme): MedAppColors {
    val heroTop = Color(0xFF0B3D39)
    val heroBottom = Color(0xFF0F766E)
    return MedAppColors(
        heroGradient = Brush.verticalGradient(listOf(heroTop, heroBottom)),
        onHero = Color(0xFFE8FFFC),
        onHeroMuted = Color(0xFFB8EDE6),
        cardOnHero = scheme.surfaceContainerHigh,
        cardOnHeroSecondary = scheme.surfaceContainer,
        accentRing = scheme.primary,
        accentRingSoft = scheme.primaryContainer,
        scannerIdle = scheme.tertiary,
        scannerBusy = scheme.secondary,
        tabPlanSelected = scheme.primary,
        tabDiarySelected = Color(0xFF047857),
        tabNextSelected = Color(0xFF6D28D9),
        tabUnselected = Color(0xFF64748B),
        snackbarSuccess = Color(0xFF047857),
        snackbarInfo = Color(0xFF0369A1),
        onSnackbar = Color(0xFFFFFFFF),
        illustrationPackDeep = Color(0xFF115E59),
        illustrationPackMid = Color(0xFF0D9488),
        illustrationPackAccent = Color(0xFF5EEAD4),
        illustrationPillGreenStart = Color(0xFF34D399),
        illustrationPillGreenEnd = Color(0xFF059669)
    )
}

private fun medAppColorsDark(scheme: ColorScheme): MedAppColors {
    val heroTop = Color(0xFF020617)
    val heroBottom = Color(0xFF134E4A)
    return MedAppColors(
        heroGradient = Brush.verticalGradient(listOf(heroTop, heroBottom)),
        onHero = Color(0xFFCCFBF1),
        onHeroMuted = Color(0xFF94D6CC),
        cardOnHero = scheme.surfaceContainerHigh,
        cardOnHeroSecondary = scheme.surfaceContainer,
        accentRing = scheme.primary,
        accentRingSoft = scheme.primaryContainer,
        scannerIdle = scheme.tertiary,
        scannerBusy = scheme.secondary,
        tabPlanSelected = scheme.primary,
        tabDiarySelected = Color(0xFF34D399),
        tabNextSelected = Color(0xFFC4B5FD),
        tabUnselected = Color(0xFF94A3B8),
        snackbarSuccess = Color(0xFF059669),
        snackbarInfo = Color(0xFF0284C7),
        onSnackbar = Color(0xFFFFFFFF),
        illustrationPackDeep = Color(0xFF042F2E),
        illustrationPackMid = Color(0xFF14B8A6),
        illustrationPackAccent = Color(0xFF99F6E4),
        illustrationPillGreenStart = Color(0xFF6EE7B7),
        illustrationPillGreenEnd = Color(0xFF10B981)
    )
}
