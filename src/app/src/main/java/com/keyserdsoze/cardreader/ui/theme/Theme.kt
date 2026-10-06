package com.keyserdsoze.cardreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF245493),
    secondary = Color(0xFF3E647D),
    tertiary = Color(0xFF735572),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    secondary = Color(0xFFA6C8E4),
    tertiary = Color(0xFFE0B9DB),
)

@Composable
fun CardReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
