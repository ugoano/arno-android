package network.arno.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisBg,
    secondary = JarvisGreen,
    tertiary = JarvisYellow,
    background = JarvisBg,
    surface = JarvisSurface,
    surfaceVariant = JarvisSurfaceVariant,
    onBackground = JarvisText,
    onSurface = JarvisText,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorder,
    error = JarvisRed,
)

@Composable
fun ArnoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = JarvisBg.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = JarvisBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = ArnoTypography,
        content = content,
    )
}
