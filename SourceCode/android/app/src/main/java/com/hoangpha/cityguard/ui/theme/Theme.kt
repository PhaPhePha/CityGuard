package com.hoangpha.cityguard.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CityGuardColorScheme = lightColorScheme(
    primary = CityGuardYellow,
    onPrimary = CityGuardBlack,
    secondary = CityGuardYellowLight,
    background = CityGuardWhite,
    surface = CityGuardWhite,
    onSurface = CityGuardBlack
)

@Suppress("DEPRECATION")
@Composable
fun CityGuardAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CityGuardYellowLight.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = CityGuardColorScheme,
        typography = Typography(),
        content = content
    )
}