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
  primary = GreenPrimary,
  onPrimary = GreenOnPrimary,
  primaryContainer = GreenPrimaryContainer,
  onPrimaryContainer = GreenOnPrimaryContainer,
  secondary = GreenSecondary,
  onSecondary = GreenOnSecondary,
  secondaryContainer = GreenSecondaryContainer,
  onSecondaryContainer = GreenOnSecondaryContainer,
  background = GreenBackground,
  onBackground = GreenOnBackground,
  surface = GreenSurface,
  onSurface = GreenOnSurface,
  surfaceVariant = GreenSurfaceVariant,
  onSurfaceVariant = GreenOnSurfaceVariant,
  outline = GreenOutline,
  outlineVariant = GreenOutlineVariant
)

private val LightColorScheme = lightColorScheme(
  primary = GreenPrimary,
  onPrimary = GreenOnPrimary,
  primaryContainer = GreenPrimaryContainer,
  onPrimaryContainer = GreenOnPrimaryContainer,
  secondary = GreenSecondary,
  onSecondary = GreenOnSecondary,
  secondaryContainer = GreenSecondaryContainer,
  onSecondaryContainer = GreenOnSecondaryContainer,
  background = GreenBackground,
  onBackground = GreenOnBackground,
  surface = GreenSurface,
  onSurface = GreenOnSurface,
  surfaceVariant = GreenSurfaceVariant,
  onSurfaceVariant = GreenOnSurfaceVariant,
  outline = GreenOutline,
  outlineVariant = GreenOutlineVariant
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set to false to preserve the Geometric Balance brand theme
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
