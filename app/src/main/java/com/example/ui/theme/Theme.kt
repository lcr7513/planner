package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = EduIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFEEF2FF),
    secondary = EduTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = EduBackgroundDark,
    surface = EduSurfaceDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    error = EduRedAlert
)

private val LightColorScheme = lightColorScheme(
    primary = EduIndigoPrimary,
    onPrimary = EduIndigoOnPrimary,
    primaryContainer = EduIndigoContainer,
    onPrimaryContainer = EduIndigoOnContainer,
    secondary = EduTealSecondary,
    onSecondary = EduTealOnSecondary,
    secondaryContainer = EduTealContainer,
    onSecondaryContainer = EduTealOnContainer,
    background = EduBackgroundLight,
    surface = EduSurfaceLight,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = EduRedAlert
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our intentional educational palette
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
