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

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberViolet,
    secondary = CyberPink,
    tertiary = GoldMoji,
    background = CosmicDark,
    surface = MetallicSurface,
    onBackground = TextLight,
    onSurface = TextLight,
    onPrimary = TextLight,
    onSecondary = TextLight
  )

private val LightColorScheme = DarkColorScheme // Default to dark theme for maximum social look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode as requested by user
  dynamicColor: Boolean = false, // Disable dynamic colors to keep brand colors intact
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
