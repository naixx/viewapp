package com.github.naixx.viewapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ViewAppTheme(
    darkTheme: Boolean = true,
    // The platform's own palette where it has one (Android 12+); the fixed schemes elsewhere.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = (if (dynamicColor) dynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** The platform's dynamic palette ("Material You" on Android 12+), or null where there is none. */
@Composable
internal expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
