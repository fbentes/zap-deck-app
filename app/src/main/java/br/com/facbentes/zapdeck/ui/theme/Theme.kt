package br.com.facbentes.zapdeck.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = ZapDeckPrimary,
    secondary = Color(0xFF38BDF8),
    tertiary = Pink80,
    background = BlueDeepNavy,
    surface = BlueRoyalSlate,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFE2E8F0)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ZapDeckPrimary,
    onPrimary = ZapDeckOnPrimary,
    primaryContainer = ZapDeckPrimaryContainer,
    onPrimaryContainer = ZapDeckOnPrimaryContainer,
    secondary = Slate700,
    onSecondary = Color.White,
    tertiary = Pink40,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate700,
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set false to preserve strong ZapDeck contrast instead of washed pastel system tints
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
