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
  primary = SageGreen80,
  onPrimary = SageGreenDark,
  primaryContainer = SageGreen,
  onPrimaryContainer = SageGreenLight,
  secondary = Eucalyptus80,
  onSecondary = Color.Black,
  secondaryContainer = EucalyptusSecondary,
  tertiary = Terracotta80,
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
  primary = SageGreen,
  onPrimary = Color.White,
  primaryContainer = SageGreenLight,
  onPrimaryContainer = SageGreenDark,
  secondary = EucalyptusSecondary,
  onSecondary = Color.White,
  secondaryContainer = EucalyptusSecondaryContainer,
  tertiary = TerracottaAccent,
  onTertiary = Color.White,
  tertiaryContainer = TerracottaContainer,
  background = CalmBackground,
  surface = CalmSurface,
  surfaceVariant = CalmSurfaceVariant,
  onBackground = Color(0xFF18201D),
  onSurface = Color(0xFF18201D)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent soothing palette by default
  content: @Composable () -> Unit,
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
