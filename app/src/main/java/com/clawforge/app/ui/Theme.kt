package com.clawforge.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base
val BgDeep      = Color(0xFF09090B)
val BgCard      = Color(0xFF111117)
val BgElevated  = Color(0xFF1A1A24)
val BorderColor = Color(0xFF27272F)

// Brand
val BrandIndigo = Color(0xFF6366F1)
val BrandCyan   = Color(0xFF22D3EE)
val BrandViolet = Color(0xFF7C3AED)

// Text
val TextPrimary   = Color(0xFFF4F4F5)
val TextSecondary = Color(0xFF71717A)
val TextMuted     = Color(0xFF3F3F46)

// Status
val StatusGreen = Color(0xFF4ADE80)
val StatusRed   = Color(0xFFF87171)
val StatusAmber = Color(0xFFFBBF24)

// Gradients
val GradientUser   = Brush.linearGradient(listOf(BrandViolet, BrandIndigo))
val GradientBrand  = Brush.linearGradient(listOf(BrandCyan, BrandIndigo))
val GradientBrand2 = Brush.linearGradient(listOf(BrandIndigo, BrandViolet))

@Composable
fun ClawForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BrandIndigo,
            onPrimary = Color.White,
            secondary = BrandCyan,
            onSecondary = Color.Black,
            background = BgDeep,
            onBackground = TextPrimary,
            surface = BgCard,
            onSurface = TextPrimary,
            surfaceVariant = BgElevated,
            onSurfaceVariant = TextSecondary,
            outline = BorderColor
        ),
        content = content
    )
}
