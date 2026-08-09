package com.tidefetch.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tidefetch.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0969C8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E9FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF35618A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E6FF),
    onSecondaryContainer = Color(0xFF0A1E31),
    tertiary = Color(0xFF006B73),
    onTertiary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF17202B),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF17202B),
    surfaceVariant = Color(0xFFE4EBF4),
    onSurfaceVariant = Color(0xFF414A56),
    outline = Color(0xFF727C89),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CC2FF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A7D),
    onPrimaryContainer = Color(0xFFD2E7FF),
    secondary = Color(0xFFA0C9F2),
    onSecondary = Color(0xFF073353),
    secondaryContainer = Color(0xFF214A6B),
    onSecondaryContainer = Color(0xFFD2E6FF),
    tertiary = Color(0xFF58D7E2),
    onTertiary = Color(0xFF00363B),
    background = Color(0xFF08111C),
    onBackground = Color(0xFFDDE7F4),
    surface = Color(0xFF0D1826),
    onSurface = Color(0xFFDDE7F4),
    surfaceVariant = Color(0xFF283442),
    onSurfaceVariant = Color(0xFFC0C9D5),
    outline = Color(0xFF8A94A1),
    error = Color(0xFFFFB4AB),
)

@Composable
fun TideFetchTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
