package com.okeho.mapping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = White,
    primaryContainer = Green300,
    onPrimaryContainer = Black,
    secondary = Green500,
    onSecondary = White,
    secondaryContainer = Green300,
    onSecondaryContainer = Black,
    tertiary = Yellow500,
    onTertiary = Black,
    background = Gray100,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    error = Red500,
    onError = White
)

@Composable
fun OkehoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
