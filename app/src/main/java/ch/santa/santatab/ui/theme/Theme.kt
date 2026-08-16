package ch.santa.santatab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Sage80,
    onPrimary = Sage20,
    primaryContainer = Sage40,
    onPrimaryContainer = Sage90,
    secondary = Amber80,
    onSecondary = Amber20,
    secondaryContainer = Sage40,
    onSecondaryContainer = Sage90,
    tertiary = Clay80,
    onTertiary = Clay40,
    background = DarkBg,
    onBackground = Color(0xFFE3E4DC),
    surface = DarkSurface,
    onSurface = Color(0xFFE3E4DC),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFBBC3B6),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
)

private val LightColors = lightColorScheme(
    primary = Sage40,
    onPrimary = Color.White,
    primaryContainer = Sage90,
    onPrimaryContainer = Sage20,
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage20,
    tertiary = Clay40,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF1A1C18),
    surface = LightSurface,
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = Color(0xFF44483F),
    surfaceContainer = LightSurfaceHigh,
    surfaceContainerHigh = LightSurfaceHighest,
    surfaceContainerHighest = LightSurfaceHighest,
)

@Composable
fun SantaTabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Feste Markenfarben statt Material-You, damit die App überall gleich aussieht.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
