package io.github.novanity99.wallpaperselector.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.novanity99.wallpaperselector.data.THEME_DARK
import io.github.novanity99.wallpaperselector.data.THEME_LIGHT

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9A5FF), onPrimary = Color(0xFF24105E),
    primaryContainer = Color(0xFF3C2A80), onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFF6FDCC8), onSecondary = Color(0xFF00382F),
    background = Color(0xFF0F0E15), surface = Color(0xFF0F0E15),
    surfaceVariant = Color(0xFF2A2833), surfaceContainer = Color(0xFF1B1A23),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B45C2), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF), onPrimaryContainer = Color(0xFF1A0063),
    secondary = Color(0xFF006B5C), onSecondary = Color.White,
    background = Color(0xFFFCF8FF), surface = Color(0xFFFCF8FF),
    surfaceVariant = Color(0xFFE6E0EE), surfaceContainer = Color(0xFFF1ECF7),
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AppTheme(theme: Int, content: @Composable () -> Unit) {
    val dark = when (theme) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            WindowCompat.getInsetsController((view.context as Activity).window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
