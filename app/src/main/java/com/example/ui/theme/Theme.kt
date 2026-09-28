package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NakonPrimaryDark,
    onPrimary = NakonOnPrimaryDark,
    primaryContainer = NakonPrimaryContainerDark,
    onPrimaryContainer = NakonOnPrimaryContainerDark,
    secondary = NakonSecondaryDark,
    onSecondary = NakonOnSecondaryDark,
    secondaryContainer = NakonSecondaryContainerDark,
    onSecondaryContainer = NakonOnSecondaryContainerDark,
    tertiary = NakonTertiaryDark,
    onTertiary = NakonOnTertiaryDark,
    tertiaryContainer = NakonTertiaryContainerDark,
    onTertiaryContainer = NakonOnTertiaryContainerDark,
    background = NakonBackgroundDark,
    onBackground = NakonOnBackgroundDark,
    surface = NakonSurfaceDark,
    onSurface = NakonOnSurfaceDark,
    surfaceVariant = NakonSurfaceVariantDark,
    onSurfaceVariant = NakonOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = NakonPrimary,
    onPrimary = NakonOnPrimary,
    primaryContainer = NakonPrimaryContainer,
    onPrimaryContainer = NakonOnPrimaryContainer,
    secondary = NakonSecondary,
    onSecondary = NakonOnSecondary,
    secondaryContainer = NakonSecondaryContainer,
    onSecondaryContainer = NakonOnSecondaryContainer,
    tertiary = NakonTertiary,
    onTertiary = NakonOnTertiary,
    tertiaryContainer = NakonTertiaryContainer,
    onTertiaryContainer = NakonOnTertiaryContainer,
    background = NakonBackground,
    onBackground = NakonOnBackground,
    surface = NakonSurface,
    onSurface = NakonOnSurface,
    surfaceVariant = NakonSurfaceVariant,
    onSurfaceVariant = NakonOnSurfaceVariant
)

// Alias for compatibility
private val NakonPrimaryContainerCompat = NakonPrimaryContainer

@Composable
fun NakonPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Nakon brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
