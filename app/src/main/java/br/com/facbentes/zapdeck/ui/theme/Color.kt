package br.com.facbentes.zapdeck.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary brand colors (ZapDeck Teal / Emerald Green)
val ZapDeckPrimary = Color(0xFF00897B)
val ZapDeckOnPrimary = Color(0xFFFFFFFF)
val ZapDeckPrimaryContainer = Color(0xFFE0F2F1)
val ZapDeckOnPrimaryContainer = Color(0xFF004D40)

// ZapDeck Royal Blue Gradient Palette (matching the first welcome screen)
val BlueDeepNavy = Color(0xFF031B33)
val BlueRoyalSlate = Color(0xFF0D3261)
val BlueActiveVibrant = Color(0xFF16529E)

val ZapDeckBlueGradient = Brush.verticalGradient(
    colors = listOf(
        BlueDeepNavy,
        BlueRoyalSlate,
        BlueActiveVibrant
    )
)

// Text & Surfaces (Modern Executive Slate & Clean Light)
val Slate900 = Color(0xFF0F172A) // Rich, sharp headline/text color
val Slate800 = Color(0xFF1E293B) // Card headers and prominent labels
val Slate700 = Color(0xFF334155) // Secondary text color / labels
val Slate600 = Color(0xFF475569) // Body text / medium slate
val Slate500 = Color(0xFF64748B) // Subtitle / hint text color
val Slate400 = Color(0xFF94A3B8) // Disabled / muted text color / subtle icons
val Slate300 = Color(0xFFCBD5E1) // Input border / button outline
val Slate200 = Color(0xFFE2E8F0) // Refined crisp card border
val Slate100 = Color(0xFFF1F5F9) // Input / Chip background
val Slate50 = Color(0xFFF8FAFC)  // Clean, comfortable canvas background
val SoftCardBg = Color(0xFFFFFFFF) // Pure white card surface
val ChipAccentBg = Color(0xFFE0F2FE) // Soft sky chip background
val ChipAccentText = Color(0xFF0369A1) // Deep ocean chip text

// Backwards compatibility tokens
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF00897B)
val PurpleGrey40 = Color(0xFF334155)
val Pink40 = Color(0xFFE1306C)
