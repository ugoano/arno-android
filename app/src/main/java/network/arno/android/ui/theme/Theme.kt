package network.arno.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ArnoDarkColorScheme = darkColorScheme(
    primary = ArnoAccent,
    onPrimary = ArnoDark,
    secondary = ArnoGreen,
    tertiary = ArnoYellow,
    background = ArnoDark,
    surface = ArnoSurface,
    surfaceVariant = ArnoSurfaceVariant,
    onBackground = ArnoText,
    onSurface = ArnoText,
    onSurfaceVariant = ArnoTextSecondary,
    outline = ArnoBorder,
    error = ArnoRed,
)

@Composable
fun ArnoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ArnoDark.toArgb()
            window.navigationBarColor = ArnoDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = ArnoDarkColorScheme,
        typography = ArnoTypography,
        content = content,
    )
}
