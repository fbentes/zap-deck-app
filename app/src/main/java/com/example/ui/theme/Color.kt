package com.example.ui.theme

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

// Text & Surfaces (High contrast for light theme)
val Slate900 = Color(0xFF0F172A) // Rich, sharp text color
val Slate700 = Color(0xFF334155) // Secondary text color
val Slate500 = Color(0xFF64748B) // Subtitle / hint text color
val Slate400 = Color(0xFF94A3B8) // Disabled / muted text color
val Slate200 = Color(0xFFE2E8F0) // Subtle border color
val Slate100 = Color(0xFFF1F5F9) // Input background
val Slate50 = Color(0xFFF8FAFC)  // Clean card/screen background

// Backwards compatibility tokens
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF00897B)
val PurpleGrey40 = Color(0xFF334155)
val Pink40 = Color(0xFFE1306C)
