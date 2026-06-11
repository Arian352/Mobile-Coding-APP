package com.clawforge.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFFB388FF)
val DeepBg = Color(0xFF0A0E14)
val SurfaceDark = Color(0xFF101826)
val TextLight = Color(0xFFE6EDF3)
val TextDim = Color(0xFF8B98A9)

@Composable
fun ClawForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeonCyan,
            onPrimary = Color.Black,
            secondary = NeonPurple,
            onSecondary = Color.Black,
            background = DeepBg,
            onBackground = TextLight,
            surface = SurfaceDark,
            onSurface = TextLight,
            surfaceVariant = SurfaceDark,
            onSurfaceVariant = TextDim
        ),
        content = content
    )
}
