package com.foodconnect.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = NeutralWhite,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Orange40,
    onSecondary = NeutralWhite,
    secondaryContainer = Orange90,
    onSecondaryContainer = Orange40,
    tertiary = Teal40,
    onTertiary = NeutralWhite,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal40,
    error = Red40,
    errorContainer = Red90,
    onError = NeutralWhite,
    background = NeutralWhite,
    onBackground = Neutral10,
    surface = NeutralWhite,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    outline = Neutral90,
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Orange80,
    onSecondary = Orange40,
    secondaryContainer = Orange40,
    onSecondaryContainer = Orange90,
    tertiary = Teal80,
    onTertiary = Teal40,
    tertiaryContainer = Teal40,
    onTertiaryContainer = Teal90,
    error = Red80,
    errorContainer = Red40,
    onError = Neutral10,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
    outline = Neutral20,
)

@Composable
fun FoodConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
