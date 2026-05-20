package com.example.medtest1.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class UiMetrics(
    val isCompact: Boolean,
    val onboardingLogoSize: Dp,
    val onboardingAnimHeight: Dp,
    val bottomBarHeight: Dp,
    val bottomBarIconSize: Dp,
    val homeActionButtonHeight: Dp,
    val homeActionIconSize: Dp,
    val profilePhotoSize: Dp,
    val dayCardWidth: Dp,
    val statusCardWidth: Dp,
    val statusCardHeight: Dp,
    val statusIconSize: Dp,
    val savedAnimHeight: Dp,
    val completedAnimHeight: Dp
)

@Composable
fun rememberUiMetrics(): UiMetrics {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompact = screenWidthDp < 380

    return remember(screenWidthDp) {
        UiMetrics(
            isCompact = isCompact,
            onboardingLogoSize = if (isCompact) 160.dp else 188.dp,
            onboardingAnimHeight = if (isCompact) 160.dp else 190.dp,
            bottomBarHeight = if (isCompact) 72.dp else 78.dp,
            bottomBarIconSize = if (isCompact) 22.dp else 26.dp,
            homeActionButtonHeight = if (isCompact) 96.dp else 104.dp,
            homeActionIconSize = if (isCompact) 30.dp else 34.dp,
            profilePhotoSize = if (isCompact) 80.dp else 96.dp,
            dayCardWidth = if (isCompact) 154.dp else 168.dp,
            statusCardWidth = if (isCompact) 98.dp else 108.dp,
            statusCardHeight = if (isCompact) 90.dp else 96.dp,
            statusIconSize = if (isCompact) 24.dp else 28.dp,
            savedAnimHeight = if (isCompact) 88.dp else 110.dp,
            completedAnimHeight = if (isCompact) 56.dp else 70.dp
        )
    }
}
