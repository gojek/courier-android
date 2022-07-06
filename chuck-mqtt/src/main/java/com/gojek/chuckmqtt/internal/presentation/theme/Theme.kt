package com.gojek.chuckmqtt.internal.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Grey,
    primaryVariant = Purple700,
    surface = Color.Black,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Blue,
    primaryVariant = Purple700,
    surface = Color.White,
    secondary = Grey
)

@Composable
internal fun ChuckMqttTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
